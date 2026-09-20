package com.example.buildpro.service.impl;

import com.example.buildpro.entity.ProjectItem;
import com.example.buildpro.repository.ProjectItemRepository;
import com.example.buildpro.service.ProjectItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectItemServiceImpl implements ProjectItemService {

    private final ProjectItemRepository projectItemRepository;

    @Override
    public List<ProjectItem> findAll() {
        return projectItemRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<ProjectItem> findById(Long id) {
        return projectItemRepository.findById(id);
    }

    @Override
    public ProjectItem create(ProjectItem project) {
        return projectItemRepository.save(project);
    }

    @Override
    public Optional<ProjectItem> update(Long id, ProjectItem update) {
        return projectItemRepository.findById(id).map(existing -> {
            existing.setImageUrl(update.getImageUrl());
            existing.setTitle(update.getTitle());
            existing.setDisplayOrder(update.getDisplayOrder());

            // If the incoming imageUrl isn't this project's own upload endpoint,
            // the admin is pointing it at an external URL (or clearing it) - drop
            // any previously uploaded bytes rather than leaving them orphaned in
            // the database, and so a later re-upload starts clean.
            String ownImagePath = "/api/projects/" + id + "/image";
            if (!ownImagePath.equals(update.getImageUrl())) {
                existing.setImageData(null);
                existing.setImageContentType(null);
            }
            return projectItemRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!projectItemRepository.existsById(id)) {
            return false;
        }
        projectItemRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<ProjectItem> storeImage(Long id, byte[] data, String contentType) {
        return projectItemRepository.findById(id).map(existing -> {
            existing.setImageData(data);
            existing.setImageContentType(contentType);
            existing.setImageUrl("/api/projects/" + id + "/image");
            return projectItemRepository.save(existing);
        });
    }
}
