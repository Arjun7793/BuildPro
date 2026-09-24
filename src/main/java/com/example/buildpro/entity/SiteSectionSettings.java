package com.example.buildpro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// The "master config" for which sections of the public site are turned on -
// a singleton row (same shape as CompanyInfo/HeroSection/AboutSection: one
// record, edited via PUT, never really "deleted" from the admin UI - see the
// `singleton` flag on the matching SECTIONS entry in admin/content.html).
// Read via /api/content (SiteContentResponse.sectionSettings) by every public
// page, which hides the matching <section> (and, for Home/About/Services/
// Projects/Contact, the matching nav link) when its flag is false. See
// applySectionVisibility() in index.html/projects.html/testimonials.html.
@Entity
@Table(name = "site_section_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteSectionSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "showHome is required")
    @Column(name = "show_home", nullable = false)
    private Boolean showHome = Boolean.TRUE;

    @NotNull(message = "showAbout is required")
    @Column(name = "show_about", nullable = false)
    private Boolean showAbout = Boolean.TRUE;

    @NotNull(message = "showServices is required")
    @Column(name = "show_services", nullable = false)
    private Boolean showServices = Boolean.TRUE;

    @NotNull(message = "showStats is required")
    @Column(name = "show_stats", nullable = false)
    private Boolean showStats = Boolean.TRUE;

    @NotNull(message = "showProjects is required")
    @Column(name = "show_projects", nullable = false)
    private Boolean showProjects = Boolean.TRUE;

    @NotNull(message = "showSamplePlans is required")
    @Column(name = "show_sample_plans", nullable = false)
    private Boolean showSamplePlans = Boolean.TRUE;

    @NotNull(message = "showTestimonials is required")
    @Column(name = "show_testimonials", nullable = false)
    private Boolean showTestimonials = Boolean.TRUE;

    @NotNull(message = "showContact is required")
    @Column(name = "show_contact", nullable = false)
    private Boolean showContact = Boolean.TRUE;
}
