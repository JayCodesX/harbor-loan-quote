package com.jaycodesx.mortgage.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for notification-service (consumer side).
 * Declares the exchange, durable queue, and binding for RateSheetActivated events.
 *
 * notification-service declares all three components — exchange, queue, and binding —
 * because AMQP requires the consumer to declare the full topology before listening.
 * pricing-service declares the exchange independently; AMQP is idempotent on re-declaration
 * of durable exchanges with matching arguments.
 *
 * Conditional on app.rabbitmq.consumer.enabled=true so this config is not loaded
 * in local development or test environments where no broker is present.
 */
@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.consumer.enabled", havingValue = "true")
public class RabbitMqConsumerConfig {

    public static final String RATE_SHEET_EXCHANGE = "rate-sheet.events";
    public static final String RATE_SHEET_ACTIVATED_QUEUE = "rate-sheet.activated";
    public static final String RATE_SHEET_ACTIVATED_ROUTING_KEY = "RATE_SHEET_ACTIVATED";

    @Bean
    TopicExchange rateSheetEventsExchange() {
        return new TopicExchange(RATE_SHEET_EXCHANGE, true, false);
    }

    @Bean
    Queue rateSheetActivatedQueue() {
        return new Queue(RATE_SHEET_ACTIVATED_QUEUE, true);
    }

    @Bean
    Binding rateSheetActivatedBinding(Queue rateSheetActivatedQueue, TopicExchange rateSheetEventsExchange) {
        return BindingBuilder.bind(rateSheetActivatedQueue)
                .to(rateSheetEventsExchange)
                .with(RATE_SHEET_ACTIVATED_ROUTING_KEY);
    }

    /**
     * Configures Jackson deserialization to use INFERRED type precedence.
     * This ignores the {@code __TypeId__} header set by pricing-service (which carries
     * the pricing-service fully-qualified class name) and instead infers the target type
     * from the {@code @RabbitListener} method parameter. This is the standard pattern for
     * cross-service JSON messaging where publisher and consumer have independent class hierarchies.
     */
    @Bean
    MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
