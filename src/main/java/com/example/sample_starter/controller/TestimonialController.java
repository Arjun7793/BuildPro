package com.example.sample_starter.controller;

import com.example.sample_starter.entity.Testimonial;
import com.example.sample_starter.service.TestimonialService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/testimonials", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Testimonials", description = "Client testimonials")
public class TestimonialController {

    private final TestimonialService testimonialService;

    @GetMapping
    @Operation(summary = "List all testimonials, ordered for display")
    public List<Testimonial> getAll() {
        return testimonialService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one testimonial by id")
    public ResponseEntity<Testimonial> getOne(@PathVariable Long id) {
        return testimonialService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a testimonial")
    public ResponseEntity<Testimonial> create(@Valid @RequestBody Testimonial testimonial) {
        Testimonial saved = testimonialService.create(testimonial);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a testimonial")
    public ResponseEntity<Testimonial> update(@PathVariable Long id, @Valid @RequestBody Testimonial update) {
        return testimonialService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a testimonial")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return testimonialService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
