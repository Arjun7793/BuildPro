package com.example.buildpro.controller;

import com.example.buildpro.entity.SamplePlan;
import com.example.buildpro.exception.ResourceNotFoundException;
import com.example.buildpro.service.SamplePlanService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/sample-plans", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Sample Plans", description = "2D sketches and 3D animations shown under Projects")
public class SamplePlanController {

    private final SamplePlanService samplePlanService;

    @GetMapping
    @Operation(summary = "List all sample plans, ordered for display")
    public List<SamplePlan> getAll() {
        return samplePlanService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one sample plan by id")
    public ResponseEntity<SamplePlan> getOne(@PathVariable Long id) {
        return samplePlanService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a sample plan")
    public ResponseEntity<SamplePlan> create(@Valid @RequestBody SamplePlan plan) {
        SamplePlan saved = samplePlanService.create(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a sample plan")
    public ResponseEntity<SamplePlan> update(@PathVariable Long id, @Valid @RequestBody SamplePlan update) {
        return samplePlanService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a sample plan")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return samplePlanService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // Image upload/serving - stored as bytes in the database (see SamplePlan),
    // same as project photos. See the identical comment on ProjectController.

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image for a sample plan - replaces its imageUrl with this plan's own "
            + "serving endpoint (admin only - requires login)")
    public ResponseEntity<SamplePlan> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        return samplePlanService.storeImage(id, file.getBytes(), contentType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Sample plan " + id + " not found"));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "The raw bytes of a sample plan's uploaded image, if it has one")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return samplePlanService.findById(id)
                .filter(plan -> plan.getImageData() != null && plan.getImageData().length > 0)
                .map(plan -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(plan.getImageContentType()))
                        // A day of browser/CDN caching - same rationale as ProjectController's
                        // getImage(): these photos don't change often.
                        .header("Cache-Control", "public, max-age=86400")
                        .body(plan.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
