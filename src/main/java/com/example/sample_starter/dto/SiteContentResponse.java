package com.example.sample_starter.dto;

import com.example.sample_starter.entity.CompanyInfo;
import com.example.sample_starter.entity.ProjectItem;
import com.example.sample_starter.entity.ServiceItem;
import com.example.sample_starter.entity.Stat;
import com.example.sample_starter.entity.Testimonial;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SiteContentResponse {
    private List<ServiceItem> services;
    private List<Stat> stats;
    private List<ProjectItem> projects;
    private List<Testimonial> testimonials;
    private CompanyInfo companyInfo;
}
