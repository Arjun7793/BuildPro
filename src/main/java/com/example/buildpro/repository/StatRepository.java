package com.example.buildpro.repository;

import com.example.buildpro.entity.Stat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatRepository extends JpaRepository<Stat, Long> {
    List<Stat> findAllByOrderByDisplayOrderAsc();
}
