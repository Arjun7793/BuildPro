package com.example.buildpro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "label is required")
    @Column(nullable = false)
    private String label;

    @NotNull(message = "targetValue is required")
    @Column(name = "target_value", nullable = false)
    private Integer targetValue;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Same draft/published convention as ServiceItem/ProjectItem/Testimonial:
    // defaults true so every existing row stays live after the migration,
    // filtered only at SiteContentServiceImpl's /api/content aggregation, and
    // @NotNull so an explicit null from a client 400s cleanly instead of
    // hitting the DB's NOT NULL constraint.
    @NotNull(message = "published is required")
    @Column(nullable = false)
    private Boolean published = Boolean.TRUE;
}
