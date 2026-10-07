package com.example.buildpro.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Body of POST /api/auth/token - the mobile admin app's sign-in request.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    @NotBlank(message = "username is required")
    private String username;

    // Never logged - excluded from Lombok's toString().
    @ToString.Exclude
    @NotBlank(message = "password is required")
    private String password;
}
