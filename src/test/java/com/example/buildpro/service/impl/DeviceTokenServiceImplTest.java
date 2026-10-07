package com.example.buildpro.service.impl;

import com.example.buildpro.dto.DeviceRegistrationRequest;
import com.example.buildpro.entity.DevicePlatform;
import com.example.buildpro.entity.DeviceToken;
import com.example.buildpro.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceTokenServiceImplTest {

    private final DeviceTokenRepository repository = mock(DeviceTokenRepository.class);
    private final DeviceTokenServiceImpl service = new DeviceTokenServiceImpl(repository);

    @Test
    void registeringANewTokenCreatesARow() {
        when(repository.findByToken("tok-1")).thenReturn(Optional.empty());
        when(repository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceToken saved = service.register("admin", new DeviceRegistrationRequest("tok-1", DevicePlatform.ANDROID, "Pixel 8"));

        assertEquals("tok-1", saved.getToken());
        assertEquals(DevicePlatform.ANDROID, saved.getPlatform());
        assertEquals("admin", saved.getUsername());
        assertEquals("Pixel 8", saved.getDeviceName());
    }

    @Test
    void reRegisteringAKnownTokenUpdatesInsteadOfDuplicating() {
        LocalDateTime longAgo = LocalDateTime.now().minusDays(30);
        DeviceToken existing = new DeviceToken(7L, "tok-1", DevicePlatform.IOS, "Old name", "admin", longAgo, longAgo);
        when(repository.findByToken("tok-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceToken saved = service.register("admin", new DeviceRegistrationRequest("tok-1", DevicePlatform.IOS, "Arjun's iPhone"));

        assertEquals(7L, saved.getId());
        assertEquals("Arjun's iPhone", saved.getDeviceName());
        assertTrue(saved.getLastSeenAt().isAfter(longAgo));
        assertEquals(longAgo, saved.getCreatedAt());
    }

    @Test
    void unregisterReportsMissingIds() {
        when(repository.existsById(99L)).thenReturn(false);
        assertFalse(service.unregister(99L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void removeTokensSkipsTheDatabaseWhenNothingToRemove() {
        service.removeTokens(List.of());
        verifyNoInteractions(repository);
    }
}
