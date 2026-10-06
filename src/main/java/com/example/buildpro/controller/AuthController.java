package com.example.buildpro.controller;

import com.example.buildpro.dto.AuthUserResponse;
import com.example.buildpro.dto.TokenRequest;
import com.example.buildpro.dto.TokenResponse;
import com.example.buildpro.service.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.buildpro.config.JwtConfig;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Token sign-in for the mobile admin app. The web admin pages keep using the
// session login at /admin/login; this is the stateless equivalent for a native
// app, which can't sensibly juggle session + CSRF cookies. Both check the same
// admin account (admin.username / admin.password).
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Bearer-token sign-in for the mobile admin app")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sign in with the admin username/password and get a bearer token",
            description = "Wrong credentials return 401. Rate-limited per IP (see AuthTokenRateLimitFilter).")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        // Throws an AuthenticationException on bad credentials - turned into a
        // 401 ApiError by GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword()));
        return authTokenService.issueToken(authentication);
    }

    @GetMapping("/me")
    @Operation(summary = "Who the current bearer token belongs to (requires a valid token)")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtConfig.ROLES_CLAIM);
        return new AuthUserResponse(jwt.getSubject(), roles == null ? List.of() : roles, jwt.getExpiresAt());
    }
}
