package com.example.buildpro.service.impl;

import com.example.buildpro.dto.DeviceRegistrationRequest;
import com.example.buildpro.entity.DeviceToken;
import com.example.buildpro.repository.DeviceTokenRepository;
import com.example.buildpro.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public DeviceToken register(String username, DeviceRegistrationRequest request) {
        // Upsert on the token: the app re-registers on every launch, and a token
        // can move between accounts if someone signs out and back in as another
        // user on the same phone.
        DeviceToken device = deviceTokenRepository.findByToken(request.getToken())
                .orElseGet(() -> {
                    DeviceToken fresh = new DeviceToken();
                    fresh.setToken(request.getToken());
                    return fresh;
                });
        device.setPlatform(request.getPlatform());
        device.setDeviceName(request.getDeviceName());
        device.setUsername(username);
        device.setLastSeenAt(LocalDateTime.now());
        return deviceTokenRepository.save(device);
    }

    @Override
    @Transactional
    public boolean unregister(Long id) {
        if (!deviceTokenRepository.existsById(id)) {
            return false;
        }
        deviceTokenRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceToken> findAll() {
        return deviceTokenRepository.findAll();
    }

    @Override
    @Transactional
    public void removeTokens(Collection<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        long removed = deviceTokenRepository.deleteByTokenIn(tokens);
        log.info("Removed {} device token(s) Firebase reported as no longer valid", removed);
    }
}
