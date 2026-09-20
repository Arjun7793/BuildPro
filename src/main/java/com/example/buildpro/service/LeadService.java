package com.example.buildpro.service;

import com.example.buildpro.entity.Lead;

import java.util.List;
import java.util.Optional;

public interface LeadService {
    List<Lead> findAll();
    Optional<Lead> findById(Long id);
    Lead create(Lead lead);
    boolean delete(Long id);
}
