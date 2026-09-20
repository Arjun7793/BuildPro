package com.example.buildpro.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// The public site's very first section (the full-bleed "hero" banner right
// under the header, id="home") - headline, supporting text, the call-to-
// action button's label, and an optional background image. Singleton, same
// pattern as CompanyInfo/AboutSection: there's only ever meant to be one row,
// enforced client-side in admin/content.html (see the `singleton` flag on the
// SECTIONS entry there), not at this layer.
@Entity
@Table(name = "hero_section")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeroSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "headline is required")
    @Column(nullable = false)
    private String headline;

    @Column(length = 1000)
    private String subheading;

    @Column(name = "cta_text")
    private String ctaText;

    // Either an external URL, or - once an image has been uploaded through the
    // admin page - set automatically to this record's own
    // /api/hero-section/{id}/image endpoint (see HeroSectionController), same
    // hybrid pattern as ProjectItem.imageUrl.
    @Column(name = "background_image_url", length = 1000)
    private String backgroundImageUrl;

    // The uploaded image's raw bytes, if any - never serialized to JSON (see
    // ProjectItem.imageData for why). Served separately, raw, by
    // GET /api/hero-section/{id}/image.
    @JsonIgnore
    @Column(name = "background_image_data", columnDefinition = "bytea")
    private byte[] backgroundImageData;

    @Column(name = "background_image_content_type")
    private String backgroundImageContentType;
}
