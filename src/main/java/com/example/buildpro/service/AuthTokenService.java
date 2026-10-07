package com.example.buildpro.service;

import com.example.buildpro.dto.TokenResponse;
import org.springframework.security.core.Authentication;

public interface AuthTokenService {

    // Issues a signed bearer token for an already-authenticated user.
    TokenResponse issueToken(Authentication authentication);
}
