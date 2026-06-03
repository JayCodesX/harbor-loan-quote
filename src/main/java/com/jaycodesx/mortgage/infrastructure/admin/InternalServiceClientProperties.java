package com.jaycodesx.mortgage.infrastructure.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal-admin")
public record InternalServiceClientProperties(
        String pricingBaseUrl,
        String pricingAudience,
        String pricingScope,
        String notificationBaseUrl,
        String notificationAudience,
        String notificationScope
) {
}
