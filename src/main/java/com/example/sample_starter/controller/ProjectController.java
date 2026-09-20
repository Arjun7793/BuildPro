package com.example.sample_starter.controller;

import com.example.sample_starter.entity.ProjectItem;
import com.example.sample_starter.service.ProjectItemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Projects", description = "The project showcase images")
public class ProjectController {

    private final ProjectItemService projectItemService;

    @GetMapping
    @Operation(summary = "List all projects, ordered for display")
    public List<ProjectItem> getAll() {
        return projectItemService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one project by id")
    public ResponseEntity<ProjectItem> getOne(@PathVariable Long id) {
        return projectItemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectItem> create(@Valid @RequestBody ProjectItem project) {
        ProjectItem saved = projectItemService.create(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a project")
    public ResponseEntity<ProjectItem> update(@PathVariable Long id, @Valid @RequestBody ProjectItem update) {
        return projectItemService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return projectItemService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
