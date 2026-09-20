package com.example.buildpro.controller;

import com.example.buildpro.entity.ProjectItem;
import com.example.buildpro.exception.ResourceNotFoundException;
import com.example.buildpro.service.ProjectItemService;
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

    // Image upload/serving - stored as bytes in the database (see ProjectItem),
    // not on the server's own disk, since Railway's filesystem is wiped on every
    // redeploy. Upload requires the admin login (SecurityConfig has a dedicated
    // rule for this sub-path, since it's a POST that the base "/api/projects"
    // POST-requires-auth rule doesn't itself cover); serving the image back out
    // stays public, same as every other GET here - the live site needs to load it.

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image for a project - replaces its imageUrl with this project's own "
            + "serving endpoint (admin only - requires login)")
    public ResponseEntity<ProjectItem> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        return projectItemService.storeImage(id, file.getBytes(), contentType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "The raw bytes of a project's uploaded image, if it has one")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return projectItemService.findById(id)
                .filter(project -> project.getImageData() != null && project.getImageData().length > 0)
                .map(project -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(project.getImageContentType()))
                        // A day of browser/CDN caching - project photos don't change
                        // often, and re-uploading gets a fresh URL only if the admin
                        // switches away and back, but that's an acceptable staleness
                        // window for how infrequently these change.
                        .header("Cache-Control", "public, max-age=86400")
                        .body(project.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
