package com.example.buildpro.repository;

import com.example.buildpro.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    Page<Lead> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByCreatedAtGreaterThanEqual(LocalDateTime since);
}
