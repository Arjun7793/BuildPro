package com.example.buildpro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "testimonials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "clientName is required")
    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(length = 1000)
    private String message;

    private Integer rating;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Draft/published staging flag - see the identical field on ServiceItem
    // for the full explanation (filtering, default, and why @NotNull rather
    // than nullable).
    @NotNull(message = "published is required")
    @Column(nullable = false)
    private Boolean published = Boolean.TRUE;
}
