package com.example.sample_starter.repository;

import com.example.sample_starter.entity.ProjectItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectItemRepository extends JpaRepository<ProjectItem, Long> {
    List<ProjectItem> findAllByOrderByDisplayOrderAsc();
}
