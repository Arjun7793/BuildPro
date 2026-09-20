package com.example.sample_starter.controller;

import com.example.sample_starter.entity.Lead;
import com.example.sample_starter.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Contact form submissions. Only create/read/delete are exposed here -
// a submitted lead generally shouldn't be silently rewritten, so there's no PUT.
@RestController
@RequestMapping(value = "/api/leads", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Contact form submissions")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @Operation(summary = "List all leads, newest first")
    public List<Lead> getAll() {
        return leadService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one lead by id")
    public ResponseEntity<Lead> getOne(@PathVariable Long id) {
        return leadService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Submit the contact form")
    public ResponseEntity<Lead> create(@Valid @RequestBody Lead lead) {
        Lead saved = leadService.create(lead);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a lead")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return leadService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
