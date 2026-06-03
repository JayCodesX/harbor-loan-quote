package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTokenAuthorizationServiceTest {

    private final UserTokenProperties properties = new UserTokenProperties(
            "internal",
            "test-user-token-secret-12345678901234567890",
            "auth-service",
            "mortgage-loan-api",
            null,
            900
    );

    private final UserTokenAuthorizationService service = new UserTokenAuthorizationService(properties);

    @Test
    void acceptsValidUserToken() {
        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject("jay@jaycodesx.dev")
                .audience().add(properties.audience()).and()
                .claim("type", "user")
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatCode(() -> service.requireAuthenticatedUser("Bearer " + token)).doesNotThrowAnyException();
        assertThatCode(() -> service.requireAdminUser("Bearer " + token)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingBearerToken() {
        assertThatThrownBy(() -> service.requireAuthenticatedUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing bearer token");
    }

    @Test
    void acceptsOidcTokenUsingConfiguredDecoder() {
        UserTokenProperties oidcProperties = new UserTokenProperties(
                "oidc",
                "unused-secret",
                "http://localhost:18080/realms/mortgage-loan-api",
                "mortgage-loan-api-web",
                "http://localhost:18080/realms/mortgage-loan-api/protocol/openid-connect/certs",
                900
        );
        JwtDecoder decoder = mock(JwtDecoder.class);
        Jwt jwt = Jwt.withTokenValue("oidc-token")
                .issuer(oidcProperties.issuer())
                .subject("jay")
                .audience(List.of("account", oidcProperties.audience()))
                .header("alg", "RS256")
                .claim("azp", oidcProperties.audience())
                .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                .build();
        when(decoder.decode("oidc-token")).thenReturn(jwt);

        UserTokenAuthorizationService oidcService = new UserTokenAuthorizationService(oidcProperties, decoder);

        assertThatCode(() -> oidcService.requireAuthenticatedUser("Bearer oidc-token")).doesNotThrowAnyException();
        assertThatCode(() -> oidcService.requireAdminUser("Bearer oidc-token")).doesNotThrowAnyException();
    }

    @Test
    void rejectsNonAdminUserForAdminPath() {
        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject("jay@jaycodesx.dev")
                .audience().add(properties.audience()).and()
                .claim("type", "user")
                .claim("role", "USER")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> service.requireAdminUser("Bearer " + token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin role required");
    }
}
