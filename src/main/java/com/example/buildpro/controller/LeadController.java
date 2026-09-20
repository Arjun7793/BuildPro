package com.example.buildpro.controller;

import com.example.buildpro.entity.Lead;
import com.example.buildpro.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Contact form submissions. Only create/read/delete are exposed here -
// a submitted lead generally shouldn't be silently rewritten, so there's no PUT.
@RestController
@RequestMapping(value = "/api/leads", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Contact form submissions")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @Operation(summary = "List leads, newest first, paginated (admin only - requires login)",
            description = "Query params: page (0-based, default 0), size (default 20, capped at 100). "
                    + "Response shape is Spring Data's paged form: {content: [...], page: {size, number, totalElements, totalPages}}.")
    public Page<Lead> getAll(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return leadService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one lead by id (admin only - requires login)")
    public ResponseEntity<Lead> getOne(@PathVariable Long id) {
        return leadService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit the contact form")
    public ResponseEntity<Lead> create(@Valid @RequestBody Lead lead) {
        Lead saved = leadService.create(lead);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a lead (admin only - requires login)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return leadService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
