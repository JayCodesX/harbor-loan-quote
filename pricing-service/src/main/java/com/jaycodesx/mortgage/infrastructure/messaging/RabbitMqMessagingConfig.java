package com.jaycodesx.mortgage.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ TopicExchange that pricing-service publishes to.
 * pricing-service is a publisher only — it does not declare queues or bindings.
 * Queue and binding declarations live in the consumers (notification-service).
 *
 * Conditional on app.transport.type=rabbitmq so this config is not loaded
 * in Phase 1 (noop) or Phase 3 (SQS) environments.
 */
@Configuration
@ConditionalOnProperty(name = "app.transport.type", havingValue = "rabbitmq")
public class RabbitMqMessagingConfig {

    public static final String RATE_SHEET_EXCHANGE = "rate-sheet.events";

    @Bean
    TopicExchange rateSheetEventsExchange() {
        return new TopicExchange(RATE_SHEET_EXCHANGE, true, false);
    }

    /**
     * Configures the RabbitTemplate to serialize message payloads as JSON.
     * Spring Boot's AMQP auto-configuration picks up this bean and applies it to the
     * auto-configured RabbitTemplate — no manual RabbitTemplate declaration needed.
     */
    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
