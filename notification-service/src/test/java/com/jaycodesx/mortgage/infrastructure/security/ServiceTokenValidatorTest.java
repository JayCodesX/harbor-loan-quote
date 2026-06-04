package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenValidatorTest {

    // Ephemeral RSA keypair generated per test run — no key material is committed.
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private final ServiceTokenProperties properties = new ServiceTokenProperties(
            Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded()),
            "mortgage-loan-api",
            "notification-service",
            "notification:write"
    );

    private final ServiceTokenValidator validator = new ServiceTokenValidator(properties);

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test RSA keypair", e);
        }
    }

    private String signedToken(String audience) {
        return Jwts.builder()
                .issuer("mortgage-loan-api")
                .subject("mortgage-loan-api")
                .audience().add(audience).and()
                .claim("scope", "notification:write")
                .claim("type", "service")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(KEY_PAIR.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void validatesExpectedNotificationToken() {
        assertThatNoException()
                .isThrownBy(() -> validator.validateNotificationToken(signedToken("notification-service")));
    }

    @Test
    void rejectsWrongAudience() {
        assertThatThrownBy(() -> validator.validateNotificationToken(signedToken("pricing-service")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audience");
    }
}
