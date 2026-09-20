package com.example.sample_starter.service;

import com.example.sample_starter.entity.Stat;

import java.util.List;
import java.util.Optional;

public interface StatService {
    List<Stat> findAll();
    Optional<Stat> findById(Long id);
    Stat create(Stat stat);
    Optional<Stat> update(Long id, Stat update);
    boolean delete(Long id);
}
