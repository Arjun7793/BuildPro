package com.example.buildpro.service.impl;

import com.example.buildpro.config.JwtConfig;
import com.example.buildpro.dto.TokenResponse;
import com.example.buildpro.service.AuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Builds and signs the mobile app's bearer tokens (HS256, see JwtConfig).
// Lifetime is app.jwt.ttl-hours (JWT_TTL_HOURS, default 24h). There are no
// refresh tokens yet - when a token expires the app gets a 401 and sends the
// admin back to the login screen.
@Service
public class AuthTokenServiceImpl implements AuthTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public AuthTokenServiceImpl(JwtEncoder jwtEncoder,
                                @Value("${app.jwt.issuer:buildpro}") String issuer,
                                @Value("${app.jwt.ttl-hours:24}") long ttlHours) {
        this(jwtEncoder, issuer, Duration.ofHours(ttlHours), Clock.systemUTC());
    }

    // For tests - lets them control the lifetime and the clock.
    AuthTokenServiceImpl(JwtEncoder jwtEncoder, String issuer, Duration ttl, Clock clock) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("app.jwt.ttl-hours must be greater than 0");
        }
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public TokenResponse issueToken(Authentication authentication) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(JwtConfig.ROLES_CLAIM, roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", ttl.toSeconds(), expiresAt);
    }
}
