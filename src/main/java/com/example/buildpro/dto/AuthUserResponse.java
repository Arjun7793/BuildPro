package com.example.buildpro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

// Response of GET /api/auth/me - who the current bearer token belongs to. The
// app calls it on launch (splash screen) to check a saved token is still valid,
// and shows the username on the Settings screen.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {
    private String username;
    private List<String> roles;
    private Instant expiresAt;
}
