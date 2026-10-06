package com.example.buildpro.dto;

import com.example.buildpro.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Body of POST /api/devices - the mobile app registering this phone for
// new-lead push alerts.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    // FCM registration token from the Firebase Messaging SDK.
    @ToString.Exclude
    @NotBlank(message = "token is required")
    @Size(max = 512, message = "token must be at most 512 characters")
    private String token;

    @NotNull(message = "platform is required (ANDROID or IOS)")
    private DevicePlatform platform;

    // Optional, shown for reference only - e.g. "Pixel 8" or "Arjun's iPhone".
    @Size(max = 100, message = "deviceName must be at most 100 characters")
    private String deviceName;
}
