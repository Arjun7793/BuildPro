package com.example.sample_starter.repository;

import com.example.sample_starter.entity.Stat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatRepository extends JpaRepository<Stat, Long> {
    List<Stat> findAllByOrderByDisplayOrderAsc();
}
