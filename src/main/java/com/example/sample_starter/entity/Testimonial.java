package com.example.sample_starter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
}
