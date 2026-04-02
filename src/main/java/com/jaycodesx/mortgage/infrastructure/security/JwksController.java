package com.jaycodesx.mortgage.infrastructure.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Exposes the issuer's public key as a JWKS document.
 *
 * Receiving services (pricing-service, notification-service) resolve this endpoint
 * to get the public key for verifying service-to-service JWTs. Zero-downtime key
 * rotation is achieved by publishing both old and new keys here simultaneously
 * during a rotation window — see ADR-0010.
 */
@RestController
public class JwksController {

    private final ServiceTokenService serviceTokenService;

    public JwksController(ServiceTokenService serviceTokenService) {
        this.serviceTokenService = serviceTokenService;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) serviceTokenService.getPublicKey();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", serviceTokenService.getKeyId(),
                "n", encoder.encodeToString(rsaPublicKey.getModulus().toByteArray()),
                "e", encoder.encodeToString(rsaPublicKey.getPublicExponent().toByteArray())
        );

        return Map.of("keys", List.of(jwk));
    }
}
