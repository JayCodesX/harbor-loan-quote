package com.jaycodesx.mortgage.ratesheet.service;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * SSE-push consumer for the rate-change event stream (ADR-0052), migrated off
 * RabbitMQ onto Kafka/Redpanda.
 *
 * <p>This is the Kafka counterpart to {@link RateSheetActivatedConsumer} (the
 * RabbitMQ listener). It joins consumer group {@code rate-change-sse} — independent
 * of the pricing-service {@code rate-change-audit} group, so audit and SSE read the
 * same topic without interfering, each at its own offset.
 *
 * <p><b>Run one, not both.</b> To avoid a double broadcast during the transition,
 * exactly one SSE consumer should be active: enable this one with
 * {@code app.kafka.enabled=true} and disable the RabbitMQ one with
 * {@code app.rabbitmq.consumer.enabled=false} (or vice-versa). Kafka is the target
 * state per ADR-0052.
 *
 * <p>Broadcasting to connected SSE sessions is naturally idempotent: a replayed
 * event simply re-notifies clients that pricing changed, which is harmless.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class RateSheetActivatedKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(RateSheetActivatedKafkaConsumer.class);

    private final RateSheetConnectionRegistry registry;

    public RateSheetActivatedKafkaConsumer(RateSheetConnectionRegistry registry) {
        this.registry = registry;
    }

    @KafkaListener(
            topics = "${app.kafka.topic:pricing.rate-sheet.activated}",
            groupId = "rate-change-sse"
    )
    public void onRateSheetActivated(RateSheetActivatedMessage message) {
        log.info("RateSheetActivatedKafkaConsumer: received [rateSheetId={}, investorId={}]",
                message.rateSheetId(), message.investorId());
        int delivered = registry.broadcast(message);
        log.info("RateSheetActivatedKafkaConsumer: broadcast complete [rateSheetId={}, delivered={}]",
                message.rateSheetId(), delivered);
    }
}
