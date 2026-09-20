package com.example.buildpro.service;

import com.example.buildpro.entity.HeroSection;

import java.util.List;
import java.util.Optional;

public interface HeroSectionService {
    List<HeroSection> findAll();
    Optional<HeroSection> findById(Long id);
    HeroSection create(HeroSection section);
    Optional<HeroSection> update(Long id, HeroSection update);
    boolean delete(Long id);
    Optional<HeroSection> storeImage(Long id, byte[] data, String contentType);
}
