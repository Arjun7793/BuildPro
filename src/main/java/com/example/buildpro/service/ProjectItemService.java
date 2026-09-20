package com.example.buildpro.service;

import com.example.buildpro.entity.ProjectItem;

import java.util.List;
import java.util.Optional;

public interface ProjectItemService {
    List<ProjectItem> findAll();
    Optional<ProjectItem> findById(Long id);
    ProjectItem create(ProjectItem project);
    Optional<ProjectItem> update(Long id, ProjectItem update);
    boolean delete(Long id);
    Optional<ProjectItem> storeImage(Long id, byte[] data, String contentType);
}
