package com.example.sample_starter.service;

import com.example.sample_starter.entity.Lead;

import java.util.List;
import java.util.Optional;

public interface LeadService {
    List<Lead> findAll();
    Optional<Lead> findById(Long id);
    Lead create(Lead lead);
    boolean delete(Long id);
}
