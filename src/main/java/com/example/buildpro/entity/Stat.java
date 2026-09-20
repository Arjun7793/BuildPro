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
}
