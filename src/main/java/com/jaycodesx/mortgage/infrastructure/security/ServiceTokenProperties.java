package com.jaycodesx.mortgage.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.service-token")
public record ServiceTokenProperties(
        String privateKey,
        String keyId,
        String issuer,
        String audience,
        String scope,
        long ttlSeconds
) {
}
