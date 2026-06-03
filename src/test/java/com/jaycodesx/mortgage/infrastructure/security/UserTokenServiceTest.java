package com.jaycodesx.mortgage.infrastructure.security;

import com.jaycodesx.mortgage.auth.model.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UserTokenServiceTest {

    @Test
    void generatesExpectedUserTokenClaims() {
        UserTokenProperties properties = new UserTokenProperties(
                "internal",
                "test-user-token-secret-12345678901234567890",
                "auth-service",
                "mortgage-loan-api",
                null,
                900
        );
        UserTokenService userTokenService = new UserTokenService(properties);
        AuthUser user = new AuthUser();
        user.setEmail("jay@jaycodesx.dev");
        user.setRole("USER");

        String token = userTokenService.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getIssuer()).isEqualTo("auth-service");
        assertThat(claims.getAudience()).contains("mortgage-loan-api");
        assertThat(claims.getSubject()).isEqualTo("jay@jaycodesx.dev");
        assertThat(claims.get("type", String.class)).isEqualTo("user");
    }
}
