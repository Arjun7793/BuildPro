package com.example.sample_starter.controller;

import com.example.sample_starter.entity.CompanyInfo;
import com.example.sample_starter.service.CompanyInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company-info")
@RequiredArgsConstructor
@Tag(name = "Company Info", description = "Company name, address, phone and email shown in Contact Us")
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;

    @GetMapping
    @Operation(summary = "List company info records (usually just one)")
    public List<CompanyInfo> getAll() {
        return companyInfoService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one company info record by id")
    public ResponseEntity<CompanyInfo> getOne(@PathVariable Long id) {
        return companyInfoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a company info record")
    public ResponseEntity<CompanyInfo> create(@RequestBody CompanyInfo info) {
        CompanyInfo saved = companyInfoService.create(info);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a company info record")
    public ResponseEntity<CompanyInfo> update(@PathVariable Long id, @RequestBody CompanyInfo update) {
        return companyInfoService.update(id, update)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a company info record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return companyInfoService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
