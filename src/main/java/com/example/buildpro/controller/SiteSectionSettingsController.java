package com.example.buildpro.controller;

import com.example.buildpro.entity.SiteSectionSettings;
import com.example.buildpro.service.SiteSectionSettingsService;
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
@RequestMapping(value = "/api/site-sections", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Site Section Settings", description = "The master on/off config for which sections appear on the public site")
public class SiteSectionSettingsController {

    private final SiteSectionSettingsService siteSectionSettingsService;

    @GetMapping
    @Operation(summary = "List section visibility records (usually just one)")
    public List<SiteSectionSettings> getAll() {
        return siteSectionSettingsService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one section visibility record by id")
    public ResponseEntity<SiteSectionSettings> getOne(@PathVariable Long id) {
        return siteSectionSettingsService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a section visibility record")
    public ResponseEntity<SiteSectionSettings> create(@Valid @RequestBody SiteSectionSettings settings) {
        SiteSectionSettings saved = siteSectionSettingsService.create(settings);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a section visibility record")
    public ResponseEntity<SiteSectionSettings> update(@PathVariable Long id, @Valid @RequestBody SiteSectionSettings update) {
        return siteSectionSettingsService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a section visibility record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return siteSectionSettingsService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
