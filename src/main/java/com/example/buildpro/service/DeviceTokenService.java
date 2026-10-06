package com.example.buildpro.service;

import com.example.buildpro.dto.DeviceRegistrationRequest;
import com.example.buildpro.entity.DeviceToken;

import java.util.Collection;
import java.util.List;

public interface DeviceTokenService {

    // Creates the registration, or refreshes it if this token is already known.
    DeviceToken register(String username, DeviceRegistrationRequest request);

    // False if there's no such registration.
    boolean unregister(Long id);

    List<DeviceToken> findAll();

    // Drops tokens Firebase reported as invalid/unregistered.
    void removeTokens(Collection<String> tokens);
}
