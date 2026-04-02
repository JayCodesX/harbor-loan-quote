package com.jaycodesx.mortgage.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification-token")
public record NotificationTokenProperties(
        String audience,
        String scope
) {
}
