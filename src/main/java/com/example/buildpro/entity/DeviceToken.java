package com.example.buildpro.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

// A phone running the mobile admin app, registered for new-lead push alerts
// (table device_tokens, changeset 008-device-tokens.yaml). The token is the
// Firebase Cloud Messaging registration token the app gets from the FCM SDK on
// Android and iOS alike.
@Entity
@Table(name = "device_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kept out of JSON responses and logs - it's a credential for pushing to
    // this phone.
    @JsonIgnore
    @ToString.Exclude
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DevicePlatform platform;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    // Admin account that registered the phone.
    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Updated every time the app re-registers (each launch), so stale phones are
    // easy to spot.
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }
}
