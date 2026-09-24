package com.example.buildpro.repository;

import com.example.buildpro.entity.SamplePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SamplePlanRepository extends JpaRepository<SamplePlan, Long> {
    List<SamplePlan> findAllByOrderByDisplayOrderAsc();
}
