package com.jaycodesx.mortgage.infrastructure.messaging.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Phase 2 transport implementation backed by RabbitMQ.
 * Publishes messages to a TopicExchange using the message destination as the exchange name
 * and the event type as the routing key.
 *
 * Active when app.transport.type=rabbitmq. Not loaded in Phase 1 (noop) or Phase 3 (sqs).
 * No business code references RabbitMQ directly — all publishing goes through MessageTransport.
 */
@Service
@ConditionalOnProperty(name = "app.transport.type", havingValue = "rabbitmq")
public class RabbitMqMessageTransport implements MessageTransport {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqMessageTransport.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqMessageTransport(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(TransportMessage message) {
        log.debug("RabbitMqMessageTransport: publishing [eventType={}, destination={}, messageId={}]",
                message.eventType(), message.destination(), message.messageId());
        rabbitTemplate.convertAndSend(message.destination(), message.eventType(), message.payload());
        log.info("RabbitMqMessageTransport: published [eventType={}, destination={}, messageId={}]",
                message.eventType(), message.destination(), message.messageId());
    }
}
