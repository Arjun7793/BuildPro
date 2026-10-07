package com.example.buildpro.service.impl;

import com.example.buildpro.config.JwtConfig;
import com.example.buildpro.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Plain unit test - no Spring context or database needed. Covers the token
// round trip (issue -> verify) and the cases the decoder must reject.
class AuthTokenServiceImplTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    private final JwtConfig config = new JwtConfig(SECRET, "buildpro");
    private final Authentication admin = UsernamePasswordAuthenticationToken.authenticated(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @Test
    void issuedTokenVerifiesAndCarriesUserAndRoles() {
        AuthTokenServiceImpl service = new AuthTokenServiceImpl(config.jwtEncoder(), "buildpro", Duration.ofHours(24), Clock.systemUTC());

        TokenResponse response = service.issueToken(admin);
        Jwt jwt = config.jwtDecoder().decode(response.getAccessToken());

        assertEquals("Bearer", response.getTokenType());
        assertEquals(Duration.ofHours(24).toSeconds(), response.getExpiresIn());
        assertEquals("admin", jwt.getSubject());
        assertEquals(List.of("ROLE_ADMIN"), jwt.getClaimAsStringList(JwtConfig.ROLES_CLAIM));
        assertEquals(response.getExpiresAt().getEpochSecond(), jwt.getExpiresAt().getEpochSecond());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtConfig other = new JwtConfig("a-completely-different-secret-32-bytes-plus", "buildpro");
        String token = new AuthTokenServiceImpl(other.jwtEncoder(), "buildpro", Duration.ofHours(1), Clock.systemUTC())
                .issueToken(admin).getAccessToken();

        assertThrows(JwtException.class, () -> config.jwtDecoder().decode(token));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String token = new AuthTokenServiceImpl(config.jwtEncoder(), "someone-else", Duration.ofHours(1), Clock.systemUTC())
                .issueToken(admin).getAccessToken();

        assertThrows(JwtException.class, () -> config.jwtDecoder().decode(token));
    }

    @Test
    void expiredTokenIsRejected() {
        Clock twoDaysAgo = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
        String token = new AuthTokenServiceImpl(config.jwtEncoder(), "buildpro", Duration.ofHours(1), twoDaysAgo)
                .issueToken(admin).getAccessToken();

        assertThrows(JwtException.class, () -> config.jwtDecoder().decode(token));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig("too-short", "buildpro"));
    }
}
