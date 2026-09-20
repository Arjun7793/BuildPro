package com.example.buildpro.service;

import com.example.buildpro.entity.Stat;

import java.util.List;
import java.util.Optional;

public interface StatService {
    List<Stat> findAll();
    Optional<Stat> findById(Long id);
    Stat create(Stat stat);
    Optional<Stat> update(Long id, Stat update);
    boolean delete(Long id);
}
