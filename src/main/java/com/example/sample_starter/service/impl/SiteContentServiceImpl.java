package com.example.sample_starter.service.impl;

import com.example.sample_starter.dto.SiteContentResponse;
import com.example.sample_starter.service.CompanyInfoService;
import com.example.sample_starter.service.ProjectItemService;
import com.example.sample_starter.service.ServiceItemService;
import com.example.sample_starter.service.SiteContentService;
import com.example.sample_starter.service.StatService;
import com.example.sample_starter.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final ServiceItemService serviceItemService;
    private final StatService statService;
    private final ProjectItemService projectItemService;
    private final TestimonialService testimonialService;
    private final CompanyInfoService companyInfoService;

    @Override
    public SiteContentResponse getContent() {
        return new SiteContentResponse(
                serviceItemService.findAll(),
                statService.findAll(),
                projectItemService.findAll(),
                testimonialService.findAll(),
                companyInfoService.findAll().stream().findFirst().orElse(null)
        );
    }
}
