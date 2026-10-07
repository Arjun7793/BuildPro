package com.example.buildpro.repository;

import com.example.buildpro.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    long deleteByTokenIn(Collection<String> tokens);
}
