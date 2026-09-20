package com.example.buildpro.service.impl;

import com.example.buildpro.entity.Lead;
import com.example.buildpro.repository.LeadRepository;
import com.example.buildpro.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;

    @Override
    public Page<Lead> findAll(Pageable pageable) {
        return leadRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Optional<Lead> findById(Long id) {
        return leadRepository.findById(id);
    }

    @Override
    public Lead create(Lead lead) {
        lead.setId(null);
        lead.setCreatedAt(null);
        return leadRepository.save(lead);
    }

    @Override
    public boolean delete(Long id) {
        if (!leadRepository.existsById(id)) {
            return false;
        }
        leadRepository.deleteById(id);
        return true;
    }
}
