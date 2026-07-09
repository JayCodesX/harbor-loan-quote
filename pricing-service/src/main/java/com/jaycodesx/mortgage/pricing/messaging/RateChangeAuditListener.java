package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.pricing.model.RateChangeAudit;
import com.jaycodesx.mortgage.pricing.repository.RateChangeAuditRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Audit consumer for the rate-change event stream (ADR-0052).
 *
 * <p>One of the independent Kafka consumer groups over
 * {@code pricing.rate-sheet.activated}. Its job is durable retention: persist a row
 * per activation so the full history of rate changes is queryable and, because it
 * reads a Kafka log, replayable — the whole topic can be re-read to rebuild this
 * table or seed a new consumer.
 *
 * <p><b>Idempotent by design.</b> Kafka is at-least-once, so this handler may see
 * the same event twice (redelivery, rebalance, or a deliberate offset replay). It
 * guards with {@code existsByRateSheetId} and additionally catches the unique-key
 * violation as a belt-and-braces race guard — either way a duplicate is a no-op,
 * not an error.
 *
 * <p>Active only when {@code app.kafka.enabled=true}; otherwise no listener is
 * registered and the service runs without the stream.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class RateChangeAuditListener {

    private static final Logger log = LoggerFactory.getLogger(RateChangeAuditListener.class);

    private final RateChangeAuditRepository auditRepository;

    public RateChangeAuditListener(RateChangeAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topic:pricing.rate-sheet.activated}",
            groupId = "rate-change-audit"
    )
    public void onRateChange(ConsumerRecord<String, RateSheetActivatedMessage> record) {
        RateSheetActivatedMessage event = record.value();

        if (auditRepository.existsByRateSheetId(event.rateSheetId())) {
            log.debug("Audit skip: rateSheetId={} already recorded (replay/redelivery)",
                    event.rateSheetId());
            return;
        }

        RateChangeAudit audit = new RateChangeAudit(
                event.rateSheetId(),
                event.investorId(),
                event.effectiveAt(),
                event.expiresAt(),
                LocalDateTime.now(),
                record.partition(),
                record.offset()
        );

        try {
            auditRepository.save(audit);
            log.info("Audited rate change [rateSheetId={}, investorId={}, partition={}, offset={}]",
                    event.rateSheetId(), event.investorId(), record.partition(), record.offset());
        } catch (DataIntegrityViolationException duplicate) {
            // A concurrent consumer/replay inserted the same rateSheetId between the
            // exists-check and the save. The unique key held the line; treat as done.
            log.debug("Audit race: rateSheetId={} inserted concurrently, ignoring",
                    event.rateSheetId());
        }
    }
}
