package com.example.buildpro.service;

import com.example.buildpro.entity.SamplePlan;

import java.util.List;
import java.util.Optional;

public interface SamplePlanService {
    List<SamplePlan> findAll();
    Optional<SamplePlan> findById(Long id);
    SamplePlan create(SamplePlan plan);
    Optional<SamplePlan> update(Long id, SamplePlan update);
    boolean delete(Long id);
    Optional<SamplePlan> storeImage(Long id, byte[] data, String contentType);
}
