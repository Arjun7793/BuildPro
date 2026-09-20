package com.example.buildpro.entity;

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

    @NotBlank(message = "imageUrl is required")
    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    private String title;

    @Column(name = "display_order")
    private Integer displayOrder;
}
