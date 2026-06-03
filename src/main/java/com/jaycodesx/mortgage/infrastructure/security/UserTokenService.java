package com.jaycodesx.mortgage.infrastructure.security;

import com.jaycodesx.mortgage.auth.model.AuthUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
@EnableConfigurationProperties(UserTokenProperties.class)
public class UserTokenService {

    private final UserTokenProperties properties;
    private final SecretKey signingKey;

    public UserTokenService(UserTokenProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .audience().add(properties.audience()).and()
                .claim("type", "user")
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.ttlSeconds())))
                .signWith(signingKey)
                .compact();
    }
}
