package com.example.buildpro.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Either an external URL, or - once an image has been uploaded through the
    // admin page - set automatically to this project's own /api/projects/{id}/image
    // endpoint (see ProjectController). No longer required at the entity level:
    // a brand-new project created specifically to have a file uploaded to it has
    // neither yet, for the moment between those two requests.
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // @NotBlank only - no @Column(nullable = false) here, unlike the other
    // content entities' title/name fields: the underlying `projects.title`
    // column has never had a NOT NULL constraint (this app has no migration
    // tool and ddl-auto is `validate`, so the JPA mapping must match whatever
    // the actual schema already is). Bean validation alone is enough to
    // reject a blank title at the API layer without needing a DB migration.
    @NotBlank(message = "title is required")
    private String title;

    @Column(name = "display_order")
    private Integer displayOrder;

    // The uploaded image's raw bytes, if any - never serialized to JSON (would
    // bloat every /api/projects and /api/content response with a base64 blob).
    // Served separately, raw, by GET /api/projects/{id}/image.
    @JsonIgnore
    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;

    @Column(name = "image_content_type")
    private String imageContentType;
}
