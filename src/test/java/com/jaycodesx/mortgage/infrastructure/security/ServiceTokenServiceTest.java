package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenServiceTest {

    // Ephemeral RSA private key generated per test run — no key material is committed.
    private static String generatePrivateKeyB64() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test RSA keypair", e);
        }
    }

    @Test
    void generatesPricingTokenWithExpectedClaims() {
        ServiceTokenProperties properties = new ServiceTokenProperties(
                generatePrivateKeyB64(),
                "harbor-test-key-1",
                "mortgage-loan-api",
                "pricing-service",
                "pricing:write",
                300L
        );
        ServiceTokenService service = new ServiceTokenService(properties);

        String token = service.generatePricingToken();

        Claims claims = Jwts.parser()
                .verifyWith((RSAPublicKey) service.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getIssuer()).isEqualTo("mortgage-loan-api");
        assertThat(claims.getSubject()).isEqualTo("mortgage-loan-api");
        assertThat(claims.getAudience()).contains("pricing-service");
        assertThat(claims.get("scope", String.class)).isEqualTo("pricing:write");
        assertThat(claims.get("type", String.class)).isEqualTo("service");
    }
}
