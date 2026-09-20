package com.example.buildpro.service;

import com.example.buildpro.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LeadService {
    Page<Lead> findAll(Pageable pageable);
    Optional<Lead> findById(Long id);
    Lead create(Lead lead);
    boolean delete(Long id);
}
