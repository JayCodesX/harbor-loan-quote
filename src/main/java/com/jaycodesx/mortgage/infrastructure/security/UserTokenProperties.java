package com.jaycodesx.mortgage.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.user-token")
public record UserTokenProperties(
        String provider,
        String secret,
        String issuer,
        String audience,
        String jwkSetUri,
        long ttlSeconds
) {
}
