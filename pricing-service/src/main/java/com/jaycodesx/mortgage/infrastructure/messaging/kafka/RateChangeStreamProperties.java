package com.jaycodesx.mortgage.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Kafka/Redpanda rate-change event stream (ADR-0052).
 *
 * <p>Bound from {@code app.kafka.*}. The {@code enabled} flag gates the whole
 * stream: when false (the local default, no Redpanda running) the producer no-ops
 * and no listeners are registered, so the service boots and the RabbitMQ work-queue
 * paths are unaffected. Broker connection itself (bootstrap servers, serializers)
 * is standard Spring Boot {@code spring.kafka.*} config.
 *
 * @param enabled whether the rate-change stream is active
 * @param topic   the topic rate-sheet activations are published to and audited from
 */
@ConfigurationProperties(prefix = "app.kafka")
public record RateChangeStreamProperties(
        boolean enabled,
        String topic
) {
    public RateChangeStreamProperties {
        if (topic == null || topic.isBlank()) {
            topic = "pricing.rate-sheet.activated";
        }
    }
}
