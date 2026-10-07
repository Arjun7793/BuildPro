package com.example.buildpro.controller;

import com.example.buildpro.dto.DeviceRegistrationRequest;
import com.example.buildpro.entity.DeviceToken;
import com.example.buildpro.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Registers phones for new-lead push alerts (see FcmPushNotificationServiceImpl).
// The mobile app calls POST on every launch while "New lead alerts" is on in
// Settings, and DELETE when the admin turns alerts off or logs out. Admin-only
// (see SecurityConfig.apiAccessRules).
@RestController
@RequestMapping(value = "/api/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Mobile admin app push-alert registration")
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register (or refresh) this phone for new-lead push alerts (admin only)",
            description = "Idempotent on the token - calling it again just updates the existing registration. "
                    + "Returns the registration id the app needs to unregister later.")
    public DeviceToken register(@Valid @RequestBody DeviceRegistrationRequest request, Authentication authentication) {
        return deviceTokenService.register(authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stop push alerts to this phone (admin only)")
    public ResponseEntity<Void> unregister(@PathVariable Long id) {
        return deviceTokenService.unregister(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
