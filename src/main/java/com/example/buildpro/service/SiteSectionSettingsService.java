package com.example.buildpro.service;

import com.example.buildpro.entity.SiteSectionSettings;

import java.util.List;
import java.util.Optional;

public interface SiteSectionSettingsService {
    List<SiteSectionSettings> findAll();
    Optional<SiteSectionSettings> findById(Long id);
    SiteSectionSettings create(SiteSectionSettings settings);
    Optional<SiteSectionSettings> update(Long id, SiteSectionSettings update);
    boolean delete(Long id);
}
