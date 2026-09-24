package com.example.buildpro.service.impl;

import com.example.buildpro.entity.SiteSectionSettings;
import com.example.buildpro.repository.SiteSectionSettingsRepository;
import com.example.buildpro.service.SiteSectionSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SiteSectionSettingsServiceImpl implements SiteSectionSettingsService {

    private final SiteSectionSettingsRepository siteSectionSettingsRepository;

    @Override
    public List<SiteSectionSettings> findAll() {
        return siteSectionSettingsRepository.findAll();
    }

    @Override
    public Optional<SiteSectionSettings> findById(Long id) {
        return siteSectionSettingsRepository.findById(id);
    }

    @Override
    public SiteSectionSettings create(SiteSectionSettings settings) {
        return siteSectionSettingsRepository.save(settings);
    }

    @Override
    public Optional<SiteSectionSettings> update(Long id, SiteSectionSettings update) {
        return siteSectionSettingsRepository.findById(id).map(existing -> {
            existing.setShowHome(update.getShowHome());
            existing.setShowAbout(update.getShowAbout());
            existing.setShowServices(update.getShowServices());
            existing.setShowStats(update.getShowStats());
            existing.setShowProjects(update.getShowProjects());
            existing.setShowSamplePlans(update.getShowSamplePlans());
            existing.setShowTestimonials(update.getShowTestimonials());
            existing.setShowContact(update.getShowContact());
            return siteSectionSettingsRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!siteSectionSettingsRepository.existsById(id)) {
            return false;
        }
        siteSectionSettingsRepository.deleteById(id);
        return true;
    }
}
