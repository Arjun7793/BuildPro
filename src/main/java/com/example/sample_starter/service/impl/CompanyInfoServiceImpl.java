package com.example.sample_starter.service.impl;

import com.example.sample_starter.entity.CompanyInfo;
import com.example.sample_starter.repository.CompanyInfoRepository;
import com.example.sample_starter.service.CompanyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CompanyInfoServiceImpl implements CompanyInfoService {

    private final CompanyInfoRepository companyInfoRepository;

    @Override
    public List<CompanyInfo> findAll() {
        return companyInfoRepository.findAll();
    }

    @Override
    public Optional<CompanyInfo> findById(Long id) {
        return companyInfoRepository.findById(id);
    }

    @Override
    public CompanyInfo create(CompanyInfo info) {
        return companyInfoRepository.save(info);
    }

    @Override
    public Optional<CompanyInfo> update(Long id, CompanyInfo update) {
        return companyInfoRepository.findById(id).map(existing -> {
            existing.setCompanyName(update.getCompanyName());
            existing.setAddress(update.getAddress());
            existing.setPhone(update.getPhone());
            existing.setEmail(update.getEmail());
            return companyInfoRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!companyInfoRepository.existsById(id)) {
            return false;
        }
        companyInfoRepository.deleteById(id);
        return true;
    }
}
