package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@EnableConfigurationProperties(ServiceTokenProperties.class)
public class ServiceTokenValidator {

    private final ServiceTokenProperties properties;
    private final RSAPublicKey verifyingKey;

    public ServiceTokenValidator(ServiceTokenProperties properties) {
        this.properties = properties;
        this.verifyingKey = loadPublicKey(properties.publicKey());
    }

    private RSAPublicKey loadPublicKey(String base64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key for service token validation", e);
        }
    }

    public void validateNotificationToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(verifyingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!properties.issuer().equals(claims.getIssuer())) {
            throw new IllegalArgumentException("Invalid token issuer");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(properties.audience())) {
            throw new IllegalArgumentException("Invalid token audience");
        }
        if (!properties.scope().equals(claims.get("scope", String.class))) {
            throw new IllegalArgumentException("Invalid token scope");
        }
        if (!"service".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }
}
