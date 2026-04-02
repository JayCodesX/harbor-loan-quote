package com.jaycodesx.mortgage.infrastructure.borrower;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.borrower-service")
public record BorrowerServiceClientProperties(
        String baseUrl,
        String audience,
        String scope
) {
}
