package com.example.buildpro.service;

import com.example.buildpro.entity.AboutSection;

import java.util.List;
import java.util.Optional;

public interface AboutSectionService {
    List<AboutSection> findAll();
    Optional<AboutSection> findById(Long id);
    AboutSection create(AboutSection section);
    Optional<AboutSection> update(Long id, AboutSection update);
    boolean delete(Long id);
    Optional<AboutSection> storeImage(Long id, byte[] data, String contentType);
}
