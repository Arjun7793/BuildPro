package com.example.buildpro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "title is required")
    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Draft/published staging flag - a draft is saved but never shown on the
    // public site (see SiteContentServiceImpl, which filters services/projects/
    // testimonials to published-only before building the /api/content response
    // the live page actually fetches). Defaults to true (published) both here
    // and at the DB column level, so a brand-new item stays live-by-default -
    // same behavior as before this feature existed - unless explicitly
    // switched to draft in the admin panel. @NotNull rather than leaving it
    // nullable: the admin UI always sends an explicit true/false, and a null
    // here would otherwise trip the DB's NOT NULL constraint as an ugly 500
    // instead of a clean 400 validation error.
    @NotNull(message = "published is required")
    @Column(nullable = false)
    private Boolean published = Boolean.TRUE;
}
