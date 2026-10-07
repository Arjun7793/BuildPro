package com.example.buildpro.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Signing + verification for the mobile admin app's bearer tokens (see
// AuthController / AuthTokenServiceImpl and the "mobile API" chain in
// SecurityConfig). One shared HMAC secret signs and verifies (HS256) - right for
// a single app that both issues and accepts its own tokens, with no third-party
// identity provider involved.
//
// The secret comes from app.jwt.secret (JWT_SECRET env var). HS256 needs at
// least 256 bits of key, so anything shorter than 32 bytes fails startup here
// instead of producing weak tokens. Rotating the secret signs every app user out
// (their existing tokens stop verifying) - that's also the emergency "revoke all
// tokens" switch.
@Configuration
public class JwtConfig {

    // Claim the token carries the user's authorities in (e.g. ["ROLE_ADMIN"]).
    public static final String ROLES_CLAIM = "roles";

    private final SecretKey secretKey;
    private final String issuer;

    public JwtConfig(@Value("${app.jwt.secret}") String secret,
                     @Value("${app.jwt.issuer:buildpro}") String issuer) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be at least 32 bytes long for HS256 token signing");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
        this.issuer = issuer;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Default validators (exp/nbf with 60s clock skew) plus: the token must
        // have been issued by this app.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    // Maps the "roles" claim straight to Spring authorities. The values are stored
    // already prefixed ("ROLE_ADMIN"), exactly as the user's authorities are named
    // at login time, so no extra prefix is added here.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
