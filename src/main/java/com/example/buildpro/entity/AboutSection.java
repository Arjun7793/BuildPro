package com.example.buildpro.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// The public site's "About Us" section (id="about") - heading, body copy and
// the side image. Singleton, same pattern as CompanyInfo/HeroSection: only
// ever meant to have one row, enforced client-side in admin/content.html
// (see the `singleton` flag on the SECTIONS entry there), not at this layer.
@Entity
@Table(name = "about_section")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AboutSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "heading is required")
    @Column(nullable = false)
    private String heading;

    @Column(length = 1000)
    private String body;

    // Either an external URL, or - once an image has been uploaded through the
    // admin page - set automatically to this record's own
    // /api/about-section/{id}/image endpoint (see AboutSectionController), same
    // hybrid pattern as ProjectItem.imageUrl.
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // The uploaded image's raw bytes, if any - never serialized to JSON (see
    // ProjectItem.imageData for why). Served separately, raw, by
    // GET /api/about-section/{id}/image.
    @JsonIgnore
    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;

    @Column(name = "image_content_type")
    private String imageContentType;
}
