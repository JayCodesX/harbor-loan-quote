package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@EnableConfigurationProperties(UserTokenProperties.class)
public class UserTokenAuthorizationService {

    private final UserTokenProperties properties;
    private final SecretKey signingKey;
    private final JwtDecoder oidcJwtDecoder;

    @Autowired
    public UserTokenAuthorizationService(UserTokenProperties properties) {
        this(properties, buildOidcJwtDecoder(properties));
    }

    UserTokenAuthorizationService(UserTokenProperties properties, JwtDecoder oidcJwtDecoder) {
        this.properties = properties;
        this.oidcJwtDecoder = oidcJwtDecoder;
        this.signingKey = usesInternalProvider(properties)
                ? Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8))
                : null;
    }

    public void requireAuthenticatedUser(String authorizationHeader) {
        resolveTokenContext(authorizationHeader);
    }

    public String extractSubject(String authorizationHeader) {
        return resolveTokenContext(authorizationHeader).subject();
    }

    public void requireAdminUser(String authorizationHeader) {
        TokenContext tokenContext = resolveTokenContext(authorizationHeader);
        if (!tokenContext.roles().contains("ADMIN")) {
            throw new IllegalArgumentException("Admin role required");
        }
    }

    private TokenContext resolveTokenContext(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (usesInternalProvider(properties)) {
            return validateInternalToken(token);
        }

        return validateOidcToken(token);
    }

    private TokenContext validateInternalToken(String token) {
        Jws<Claims> claimsJws;
        try {
            claimsJws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (io.jsonwebtoken.JwtException ex) {
            throw new IllegalArgumentException("Invalid or expired user token", ex);
        }

        Claims claims = claimsJws.getPayload();
        if (!properties.issuer().equals(claims.getIssuer())) {
            throw new IllegalArgumentException("Invalid user token issuer");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(properties.audience())) {
            throw new IllegalArgumentException("Invalid user token audience");
        }
        if (!"user".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Invalid user token type");
        }
        String role = claims.get("role", String.class);
        return new TokenContext(claims.getSubject(), role == null ? List.of() : List.of(role));
    }

    private TokenContext validateOidcToken(String token) {
        try {
            Jwt jwt = Objects.requireNonNull(oidcJwtDecoder, "OIDC JWT decoder must be configured").decode(token);
            if (!properties.issuer().equals(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null)) {
                throw new IllegalArgumentException("Invalid OIDC token issuer");
            }

            List<String> audiences = jwt.getAudience();
            String authorizedParty = jwt.getClaimAsString("azp");
            String clientId = jwt.getClaimAsString("client_id");
            boolean audienceMatch = (audiences != null && audiences.contains(properties.audience()))
                    || properties.audience().equals(authorizedParty)
                    || properties.audience().equals(clientId);
            if (!audienceMatch) {
                throw new IllegalArgumentException("Invalid OIDC token audience");
            }
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
                throw new IllegalArgumentException("Invalid OIDC token subject");
            }
            return new TokenContext(jwt.getSubject(), extractRoles(jwt));
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid OIDC token", ex);
        }
    }

    private List<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object roles = realmAccessMap.get("roles");
            if (roles instanceof Collection<?> collection) {
                return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
            }
        }

        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }

        return List.of();
    }

    private static boolean usesInternalProvider(UserTokenProperties properties) {
        return properties.provider() == null || properties.provider().isBlank()
                || "internal".equalsIgnoreCase(properties.provider());
    }

    private static JwtDecoder buildOidcJwtDecoder(UserTokenProperties properties) {
        if (usesInternalProvider(properties)) {
            return null;
        }
        if (properties.jwkSetUri() == null || properties.jwkSetUri().isBlank()) {
            throw new IllegalStateException("app.user-token.jwk-set-uri must be configured for OIDC mode");
        }
        return NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
    }

    private record TokenContext(String subject, List<String> roles) {
    }
}
