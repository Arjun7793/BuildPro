package com.example.buildpro.service.impl;

import com.example.buildpro.entity.AboutSection;
import com.example.buildpro.repository.AboutSectionRepository;
import com.example.buildpro.service.AboutSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AboutSectionServiceImpl implements AboutSectionService {

    private final AboutSectionRepository aboutSectionRepository;

    @Override
    public List<AboutSection> findAll() {
        return aboutSectionRepository.findAll();
    }

    @Override
    public Optional<AboutSection> findById(Long id) {
        return aboutSectionRepository.findById(id);
    }

    @Override
    public AboutSection create(AboutSection section) {
        return aboutSectionRepository.save(section);
    }

    @Override
    public Optional<AboutSection> update(Long id, AboutSection update) {
        return aboutSectionRepository.findById(id).map(existing -> {
            existing.setHeading(update.getHeading());
            existing.setBody(update.getBody());
            existing.setImageUrl(update.getImageUrl());

            String ownImagePath = "/api/about-section/" + id + "/image";
            if (!ownImagePath.equals(update.getImageUrl())) {
                existing.setImageData(null);
                existing.setImageContentType(null);
            }
            return aboutSectionRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!aboutSectionRepository.existsById(id)) {
            return false;
        }
        aboutSectionRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<AboutSection> storeImage(Long id, byte[] data, String contentType) {
        return aboutSectionRepository.findById(id).map(existing -> {
            existing.setImageData(data);
            existing.setImageContentType(contentType);
            existing.setImageUrl("/api/about-section/" + id + "/image");
            return aboutSectionRepository.save(existing);
        });
    }
}
