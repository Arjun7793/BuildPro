package com.example.sample_starter.service;

import com.example.sample_starter.entity.CompanyInfo;

import java.util.List;
import java.util.Optional;

public interface CompanyInfoService {
    List<CompanyInfo> findAll();
    Optional<CompanyInfo> findById(Long id);
    CompanyInfo create(CompanyInfo info);
    Optional<CompanyInfo> update(Long id, CompanyInfo update);
    boolean delete(Long id);
}
