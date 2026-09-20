package com.example.buildpro.repository;

import com.example.buildpro.entity.ProjectItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectItemRepository extends JpaRepository<ProjectItem, Long> {
    List<ProjectItem> findAllByOrderByDisplayOrderAsc();
}
