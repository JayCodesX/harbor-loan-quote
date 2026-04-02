package com.jaycodesx.mortgage.infrastructure.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal-admin")
public record InternalServiceClientProperties(
        String borrowerBaseUrl,
        String borrowerAudience,
        String borrowerScope,
        String pricingBaseUrl,
        String pricingAudience,
        String pricingScope,
        String leadBaseUrl,
        String leadAudience,
        String leadScope,
        String authBaseUrl,
        String authAudience,
        String authScope,
        String notificationBaseUrl,
        String notificationAudience,
        String notificationScope
) {
}
