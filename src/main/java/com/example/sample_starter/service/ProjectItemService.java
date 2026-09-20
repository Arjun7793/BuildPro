package com.example.sample_starter.service;

import com.example.sample_starter.entity.ProjectItem;

import java.util.List;
import java.util.Optional;

public interface ProjectItemService {
    List<ProjectItem> findAll();
    Optional<ProjectItem> findById(Long id);
    ProjectItem create(ProjectItem project);
    Optional<ProjectItem> update(Long id, ProjectItem update);
    boolean delete(Long id);
}
