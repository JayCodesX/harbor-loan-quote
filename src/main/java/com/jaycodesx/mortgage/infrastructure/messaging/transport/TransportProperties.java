package com.jaycodesx.mortgage.infrastructure.messaging.transport;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.transport")
public record TransportProperties(String type) {
    public static final String NOOP = "noop";
    public static final String RABBITMQ = "rabbitmq";
    public static final String SQS = "sqs";
}
