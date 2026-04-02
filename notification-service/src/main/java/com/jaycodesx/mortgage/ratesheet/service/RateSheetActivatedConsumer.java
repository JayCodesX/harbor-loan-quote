package com.jaycodesx.mortgage.ratesheet.service;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code RateSheetActivated} events from RabbitMQ and broadcasts them
 * to connected borrower sessions via {@link RateSheetConnectionRegistry} per ADR-0049.
 *
 * <p>Active only when {@code app.rabbitmq.consumer.enabled=true}. In local development
 * (no RabbitMQ broker) this bean is not created and no listener is registered.
 *
 * <p>The {@code Jackson2JsonMessageConverter} configured in {@code RabbitMqConsumerConfig}
 * deserializes the JSON payload using {@code TypePrecedence.INFERRED}, so the cross-service
 * {@code __TypeId__} header from pricing-service is ignored — the message is deserialized
 * directly into the local {@link RateSheetActivatedMessage} record.
 */
@Component
@ConditionalOnProperty(name = "app.rabbitmq.consumer.enabled", havingValue = "true")
public class RateSheetActivatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RateSheetActivatedConsumer.class);

    private final RateSheetConnectionRegistry registry;

    public RateSheetActivatedConsumer(RateSheetConnectionRegistry registry) {
        this.registry = registry;
    }

    @RabbitListener(queues = "#{T(com.jaycodesx.mortgage.infrastructure.messaging.RabbitMqConsumerConfig).RATE_SHEET_ACTIVATED_QUEUE}")
    public void onRateSheetActivated(RateSheetActivatedMessage message) {
        log.info("RateSheetActivatedConsumer: received [rateSheetId={}, investorId={}]",
                message.rateSheetId(), message.investorId());
        int delivered = registry.broadcast(message);
        log.info("RateSheetActivatedConsumer: broadcast complete [rateSheetId={}, delivered={}]",
                message.rateSheetId(), delivered);
    }
}
