package com.example.buildpro.service;

import com.example.buildpro.dto.LeadStats;
import com.example.buildpro.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface LeadService {
    Page<Lead> search(String name, LocalDate from, LocalDate to, Pageable pageable);
    Optional<Lead> findById(Long id);
    Lead create(Lead lead);
    boolean delete(Long id);
    LeadStats getStats();
}
