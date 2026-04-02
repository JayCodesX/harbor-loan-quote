package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Jwts;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@EnableConfigurationProperties(ServiceTokenProperties.class)
public class ServiceTokenService {

    private final ServiceTokenProperties properties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public ServiceTokenService(ServiceTokenProperties properties) {
        this.properties = properties;
        this.privateKey = loadPrivateKey(properties.privateKey());
        this.publicKey = derivePublicKey(this.privateKey);
    }

    public String generatePricingToken() {
        return generateToken(properties.audience(), properties.scope());
    }

    public String generateToken(String audience, String scope) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(properties.keyId()).and()
                .issuer(properties.issuer())
                .subject(properties.issuer())
                .audience().add(audience).and()
                .claim("scope", scope)
                .claim("type", "service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.ttlSeconds())))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return properties.keyId();
    }

    private static PrivateKey loadPrivateKey(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA private key from config", ex);
        }
    }

    private static PublicKey derivePublicKey(PrivateKey privateKey) {
        try {
            RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;
            RSAPublicKeySpec spec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to derive RSA public key from private key", ex);
        }
    }
}
