package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenValidatorTest {

    // Ephemeral RSA keypair generated per test run — no key material is committed.
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private final ServiceTokenProperties properties = new ServiceTokenProperties(
            Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded()),
            "mortgage-loan-api",
            "pricing-service",
            "pricing:write"
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

    private String signedToken(String scope) {
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(properties.issuer())
                .audience().add(properties.audience()).and()
                .claim("scope", scope)
                .claim("type", "service")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(KEY_PAIR.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void acceptsValidServiceToken() {
        assertThatCode(() -> validator.validatePricingToken(signedToken("pricing:write")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTokenWithWrongScope() {
        assertThatThrownBy(() -> validator.validatePricingToken(signedToken("pricing:read")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid service token scope");
    }
}
