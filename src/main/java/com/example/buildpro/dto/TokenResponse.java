package com.example.buildpro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Response of POST /api/auth/token. The app sends accessToken back on every API
// call as "Authorization: Bearer <accessToken>", and treats a 401 (or reaching
// expiresAt) as "sign in again".
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    // Seconds until the token expires, counted from when it was issued.
    private long expiresIn;
    private Instant expiresAt;
}
