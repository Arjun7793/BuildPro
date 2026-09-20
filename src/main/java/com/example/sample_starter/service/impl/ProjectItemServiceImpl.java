package com.example.sample_starter.service.impl;

import com.example.sample_starter.entity.ProjectItem;
import com.example.sample_starter.repository.ProjectItemRepository;
import com.example.sample_starter.service.ProjectItemService;
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
}
