package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.infrastructure.messaging.kafka.RateChangeStreamProperties;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes rate-sheet activations to the Kafka/Redpanda rate-change event stream
 * (ADR-0052).
 *
 * <p>This is the <b>event-stream</b> path, deliberately separate from
 * {@link RateSheetActivatedPublisher} (the RabbitMQ work-queue path). The two run
 * side by side during the transition: RabbitMQ still drives the existing
 * notification flow, while Kafka feeds the new independent consumer groups (audit,
 * and the migrated SSE push) that need retention and replay. See ADR-0052 for why
 * the fan-out belongs on a log, not a work queue.
 *
 * <p>Messages are <b>keyed by investorId</b> so all activations for one investor
 * land on the same partition and stay ordered — a later rate sheet must not be
 * processed before the earlier one it supersedes.
 *
 * <p>The publisher self-guards on {@code app.kafka.enabled}: when the stream is off
 * (local dev with no Redpanda) it no-ops, so callers need no conditional wiring.
 */
@Service
public class RateChangeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RateChangeEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RateChangeStreamProperties properties;

    public RateChangeEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                    RateChangeStreamProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void publish(RateSheetPublication publication) {
        if (!properties.enabled()) {
            log.debug("Kafka rate-change stream disabled; skipping publish [rateSheetId={}]",
                    publication.getId());
            return;
        }

        RateSheetActivatedMessage payload = new RateSheetActivatedMessage(
                publication.getId(),
                publication.getInvestorId(),
                publication.getEffectiveAt(),
                publication.getExpiresAt()
        );

        String key = publication.getInvestorId();
        log.info("Publishing rate-change event to Kafka [topic={}, key={}, rateSheetId={}]",
                properties.topic(), key, publication.getId());

        // Fire-and-forward with an async result callback. A send failure is logged
        // but does not fail the activation transaction — the RabbitMQ path and the
        // in-process cache eviction have already run; Kafka can be replayed/repaired.
        kafkaTemplate.send(properties.topic(), key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish rate-change event [rateSheetId={}]",
                                publication.getId(), ex);
                    } else {
                        log.debug("Rate-change event acked [rateSheetId={}, partition={}, offset={}]",
                                publication.getId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
