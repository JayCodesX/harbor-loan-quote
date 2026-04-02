package com.jaycodesx.mortgage.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.service-token")
public record ServiceTokenProperties(
        String publicKey,
        String issuer,
        String audience,
        String scope
) {
}
