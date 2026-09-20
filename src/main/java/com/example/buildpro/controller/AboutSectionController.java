package com.example.buildpro.controller;

import com.example.buildpro.entity.AboutSection;
import com.example.buildpro.exception.ResourceNotFoundException;
import com.example.buildpro.service.AboutSectionService;
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
@RequestMapping(value = "/api/about-section", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "About Section", description = "Heading, body copy and image for the public site's About Us section")
public class AboutSectionController {

    private final AboutSectionService aboutSectionService;

    @GetMapping
    @Operation(summary = "List about section records (usually just one)")
    public List<AboutSection> getAll() {
        return aboutSectionService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one about section record by id")
    public ResponseEntity<AboutSection> getOne(@PathVariable Long id) {
        return aboutSectionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an about section record")
    public ResponseEntity<AboutSection> create(@Valid @RequestBody AboutSection section) {
        AboutSection saved = aboutSectionService.create(section);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an about section record")
    public ResponseEntity<AboutSection> update(@PathVariable Long id, @Valid @RequestBody AboutSection update) {
        return aboutSectionService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an about section record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return aboutSectionService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // Image upload/serving - stored as bytes in the database (see AboutSection),
    // not on the server's own disk, since Railway's filesystem is wiped on every
    // redeploy. Same pattern as ProjectController's image endpoints. Upload
    // requires the admin login (SecurityConfig has a dedicated rule for this
    // sub-path); serving the image back out stays public - the live site needs
    // to load it.

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the About Us image - replaces imageUrl with this record's own "
            + "serving endpoint (admin only - requires login)")
    public ResponseEntity<AboutSection> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        return aboutSectionService.storeImage(id, file.getBytes(), contentType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("About section " + id + " not found"));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "The raw bytes of the about section's uploaded image, if it has one")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return aboutSectionService.findById(id)
                .filter(section -> section.getImageData() != null && section.getImageData().length > 0)
                .map(section -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(section.getImageContentType()))
                        .header("Cache-Control", "public, max-age=86400")
                        .body(section.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
