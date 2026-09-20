package com.example.buildpro.controller;

import com.example.buildpro.entity.ServiceItem;
import com.example.buildpro.service.ServiceItemService;
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
@RequestMapping(value = "/api/services", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Services", description = "The services shown in the Our Services section")
public class ServiceController {

    private final ServiceItemService serviceItemService;

    @GetMapping
    @Operation(summary = "List all services, ordered for display")
    public List<ServiceItem> getAll() {
        return serviceItemService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one service by id")
    public ResponseEntity<ServiceItem> getOne(@PathVariable Long id) {
        return serviceItemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a service")
    public ResponseEntity<ServiceItem> create(@Valid @RequestBody ServiceItem service) {
        ServiceItem saved = serviceItemService.create(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a service")
    public ResponseEntity<ServiceItem> update(@PathVariable Long id, @Valid @RequestBody ServiceItem update) {
        return serviceItemService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a service")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return serviceItemService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
