package com.example.buildpro.service.impl;

import com.example.buildpro.entity.Stat;
import com.example.buildpro.repository.StatRepository;
import com.example.buildpro.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final StatRepository statRepository;

    @Override
    public List<Stat> findAll() {
        return statRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<Stat> findById(Long id) {
        return statRepository.findById(id);
    }

    @Override
    public Stat create(Stat stat) {
        return statRepository.save(stat);
    }

    @Override
    public Optional<Stat> update(Long id, Stat update) {
        return statRepository.findById(id).map(existing -> {
            existing.setLabel(update.getLabel());
            existing.setTargetValue(update.getTargetValue());
            existing.setDisplayOrder(update.getDisplayOrder());
            return statRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!statRepository.existsById(id)) {
            return false;
        }
        statRepository.deleteById(id);
        return true;
    }
}
