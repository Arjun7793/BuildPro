package com.example.buildpro.dto;

import com.example.buildpro.entity.AboutSection;
import com.example.buildpro.entity.CompanyInfo;
import com.example.buildpro.entity.HeroSection;
import com.example.buildpro.entity.ProjectItem;
import com.example.buildpro.entity.SamplePlan;
import com.example.buildpro.entity.ServiceItem;
import com.example.buildpro.entity.Stat;
import com.example.buildpro.entity.Testimonial;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SiteContentResponse {
    private HeroSection heroSection;
    private AboutSection aboutSection;
    private List<ServiceItem> services;
    private List<Stat> stats;
    private List<ProjectItem> projects;
    private List<SamplePlan> samplePlans;
    private List<Testimonial> testimonials;
    private CompanyInfo companyInfo;
}
