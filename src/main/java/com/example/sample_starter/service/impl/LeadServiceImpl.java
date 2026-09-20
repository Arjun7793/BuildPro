package com.example.sample_starter.service.impl;

import com.example.sample_starter.entity.Lead;
import com.example.sample_starter.repository.LeadRepository;
import com.example.sample_starter.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;

    @Override
    public List<Lead> findAll() {
        return leadRepository.findAllByOrderByCreatedAtDesc();
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
