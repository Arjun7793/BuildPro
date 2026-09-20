package com.example.buildpro.controller;

import com.example.buildpro.entity.HeroSection;
import com.example.buildpro.exception.ResourceNotFoundException;
import com.example.buildpro.service.HeroSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/hero-section", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Hero Section", description = "Headline, subheading, CTA label and background image for the public site's top banner")
public class HeroSectionController {

    private final HeroSectionService heroSectionService;

    @GetMapping
    @Operation(summary = "List hero section records (usually just one)")
    public List<HeroSection> getAll() {
        return heroSectionService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one hero section record by id")
    public ResponseEntity<HeroSection> getOne(@PathVariable Long id) {
        return heroSectionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a hero section record")
    public ResponseEntity<HeroSection> create(@Valid @RequestBody HeroSection section) {
        HeroSection saved = heroSectionService.create(section);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a hero section record")
    public ResponseEntity<HeroSection> update(@PathVariable Long id, @Valid @RequestBody HeroSection update) {
        return heroSectionService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a hero section record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return heroSectionService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // Image upload/serving - stored as bytes in the database (see HeroSection),
    // not on the server's own disk, since Railway's filesystem is wiped on every
    // redeploy. Same pattern as ProjectController's image endpoints. Upload
    // requires the admin login (SecurityConfig has a dedicated rule for this
    // sub-path); serving the image back out stays public - the live site needs
    // to load it.

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the hero background image - replaces backgroundImageUrl with this "
            + "record's own serving endpoint (admin only - requires login)")
    public ResponseEntity<HeroSection> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        return heroSectionService.storeImage(id, file.getBytes(), contentType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Hero section " + id + " not found"));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "The raw bytes of the hero section's uploaded background image, if it has one")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return heroSectionService.findById(id)
                .filter(section -> section.getBackgroundImageData() != null && section.getBackgroundImageData().length > 0)
                .map(section -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(section.getBackgroundImageContentType()))
                        .header("Cache-Control", "public, max-age=86400")
                        .body(section.getBackgroundImageData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
