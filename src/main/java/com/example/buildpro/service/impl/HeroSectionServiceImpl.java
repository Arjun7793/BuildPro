package com.example.buildpro.service.impl;

import com.example.buildpro.entity.HeroSection;
import com.example.buildpro.repository.HeroSectionRepository;
import com.example.buildpro.service.HeroSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HeroSectionServiceImpl implements HeroSectionService {

    private final HeroSectionRepository heroSectionRepository;

    @Override
    public List<HeroSection> findAll() {
        return heroSectionRepository.findAll();
    }

    @Override
    public Optional<HeroSection> findById(Long id) {
        return heroSectionRepository.findById(id);
    }

    @Override
    public HeroSection create(HeroSection section) {
        return heroSectionRepository.save(section);
    }

    @Override
    public Optional<HeroSection> update(Long id, HeroSection update) {
        return heroSectionRepository.findById(id).map(existing -> {
            existing.setHeadline(update.getHeadline());
            existing.setSubheading(update.getSubheading());
            existing.setCtaText(update.getCtaText());
            existing.setBackgroundImageUrl(update.getBackgroundImageUrl());

            // If the incoming URL isn't this record's own upload endpoint, the
            // admin is pointing it at an external URL (or clearing it) - drop any
            // previously uploaded bytes rather than leaving them orphaned in the
            // database, same as ProjectItemServiceImpl.update does for projects.
            String ownImagePath = "/api/hero-section/" + id + "/image";
            if (!ownImagePath.equals(update.getBackgroundImageUrl())) {
                existing.setBackgroundImageData(null);
                existing.setBackgroundImageContentType(null);
            }
            return heroSectionRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!heroSectionRepository.existsById(id)) {
            return false;
        }
        heroSectionRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<HeroSection> storeImage(Long id, byte[] data, String contentType) {
        return heroSectionRepository.findById(id).map(existing -> {
            existing.setBackgroundImageData(data);
            existing.setBackgroundImageContentType(contentType);
            existing.setBackgroundImageUrl("/api/hero-section/" + id + "/image");
            return heroSectionRepository.save(existing);
        });
    }
}
