package com.example.buildpro.service.impl;

import com.example.buildpro.entity.SamplePlan;
import com.example.buildpro.repository.SamplePlanRepository;
import com.example.buildpro.service.SamplePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SamplePlanServiceImpl implements SamplePlanService {

    private final SamplePlanRepository samplePlanRepository;

    @Override
    public List<SamplePlan> findAll() {
        return samplePlanRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<SamplePlan> findById(Long id) {
        return samplePlanRepository.findById(id);
    }

    @Override
    public SamplePlan create(SamplePlan plan) {
        return samplePlanRepository.save(plan);
    }

    @Override
    public Optional<SamplePlan> update(Long id, SamplePlan update) {
        return samplePlanRepository.findById(id).map(existing -> {
            existing.setPlanType(update.getPlanType());
            existing.setTitle(update.getTitle());
            existing.setDescription(update.getDescription());
            existing.setImageUrl(update.getImageUrl());
            existing.setVideoUrl(update.getVideoUrl());
            existing.setDisplayOrder(update.getDisplayOrder());
            existing.setPublished(update.getPublished());

            // If the incoming imageUrl isn't this plan's own upload endpoint, the
            // admin is pointing it at an external URL (or clearing it) - drop any
            // previously uploaded bytes rather than leaving them orphaned in the
            // database, and so a later re-upload starts clean. See the identical
            // comment in ProjectItemServiceImpl.update().
            String ownImagePath = "/api/sample-plans/" + id + "/image";
            if (!ownImagePath.equals(update.getImageUrl())) {
                existing.setImageData(null);
                existing.setImageContentType(null);
            }
            return samplePlanRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!samplePlanRepository.existsById(id)) {
            return false;
        }
        samplePlanRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<SamplePlan> storeImage(Long id, byte[] data, String contentType) {
        return samplePlanRepository.findById(id).map(existing -> {
            existing.setImageData(data);
            existing.setImageContentType(contentType);
            existing.setImageUrl("/api/sample-plans/" + id + "/image");
            return samplePlanRepository.save(existing);
        });
    }
}
