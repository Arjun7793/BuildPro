package com.example.sample_starter.controller;

import com.example.sample_starter.entity.Stat;
import com.example.sample_starter.service.StatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "The animated counters in the stats band")
public class StatController {

    private final StatService statService;

    @GetMapping
    @Operation(summary = "List all stats, ordered for display")
    public List<Stat> getAll() {
        return statService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one stat by id")
    public ResponseEntity<Stat> getOne(@PathVariable Long id) {
        return statService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a stat")
    public ResponseEntity<Stat> create(@RequestBody Stat stat) {
        Stat saved = statService.create(stat);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a stat")
    public ResponseEntity<Stat> update(@PathVariable Long id, @RequestBody Stat update) {
        return statService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a stat")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return statService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
