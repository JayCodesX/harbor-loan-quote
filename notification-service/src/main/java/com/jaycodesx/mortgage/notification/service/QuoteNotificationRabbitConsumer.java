package com.jaycodesx.mortgage.notification.service;

import com.jaycodesx.mortgage.infrastructure.messaging.MessageDeduplicationService;
import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code QUOTE_NOTIFICATION_SNAPSHOT} events from RabbitMQ and stores
 * the snapshot via {@link NotificationSnapshotService}, mirroring the logic of the
 * SQS-based {@link QuoteNotificationConsumer}.
 *
 * <p>Active only when {@code app.rabbitmq.consumer.enabled=true}.  The SQS consumer
 * is gated on {@code app.messaging.enabled=true}; the two gates are independent so
 * both consumers are never simultaneously active in the same deployment.
 *
 * <p>Deserialization uses the {@code Jackson2JsonMessageConverter} configured in
 * {@code RabbitMqConsumerConfig} with {@code TypePrecedence.INFERRED}, so the
 * publisher's {@code __TypeId__} header is ignored and the payload is mapped
 * directly to the local {@link QuoteNotificationMessage} record.
 */
@Component
@ConditionalOnProperty(name = "app.rabbitmq.consumer.enabled", havingValue = "true")
public class QuoteNotificationRabbitConsumer {

    private static final Logger log = LoggerFactory.getLogger(QuoteNotificationRabbitConsumer.class);

    private final ServiceTokenValidator serviceTokenValidator;
    private final MessageDeduplicationService messageDeduplicationService;
    private final NotificationSnapshotService notificationSnapshotService;

    public QuoteNotificationRabbitConsumer(
            ServiceTokenValidator serviceTokenValidator,
            MessageDeduplicationService messageDeduplicationService,
            NotificationSnapshotService notificationSnapshotService
    ) {
        this.serviceTokenValidator = serviceTokenValidator;
        this.messageDeduplicationService = messageDeduplicationService;
        this.notificationSnapshotService = notificationSnapshotService;
    }

    @RabbitListener(queues = "#{T(com.jaycodesx.mortgage.infrastructure.messaging.RabbitMqConsumerConfig).QUOTE_NOTIFICATION_QUEUE}")
    public void onQuoteNotificationSnapshot(QuoteNotificationMessage message) {
        log.info("QuoteNotificationRabbitConsumer: received [messageId={}, quoteId={}]",
                message.messageId(), message.id());

        if (!message.hasSupportedSchemaVersion()) {
            log.warn("QuoteNotificationRabbitConsumer: unsupported schema version [schemaVersion={}], discarding",
                    message.schemaVersion());
            return;
        }

        if (!messageDeduplicationService.firstReceipt("quote-notification", message.messageId())) {
            log.debug("QuoteNotificationRabbitConsumer: duplicate message [messageId={}], skipping",
                    message.messageId());
            return;
        }

        serviceTokenValidator.validateNotificationToken(message.serviceToken());

        notificationSnapshotService.store(message);

        log.info("QuoteNotificationRabbitConsumer: snapshot stored [messageId={}, quoteId={}]",
                message.messageId(), message.id());
    }
}
