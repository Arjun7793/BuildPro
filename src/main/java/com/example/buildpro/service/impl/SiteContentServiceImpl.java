package com.example.buildpro.service.impl;

import com.example.buildpro.dto.SiteContentResponse;
import com.example.buildpro.service.AboutSectionService;
import com.example.buildpro.service.CompanyInfoService;
import com.example.buildpro.service.HeroSectionService;
import com.example.buildpro.service.ProjectItemService;
import com.example.buildpro.service.ServiceItemService;
import com.example.buildpro.service.SiteContentService;
import com.example.buildpro.service.StatService;
import com.example.buildpro.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final HeroSectionService heroSectionService;
    private final AboutSectionService aboutSectionService;
    private final ServiceItemService serviceItemService;
    private final StatService statService;
    private final ProjectItemService projectItemService;
    private final TestimonialService testimonialService;
    private final CompanyInfoService companyInfoService;

    @Override
    public SiteContentResponse getContent() {
        return new SiteContentResponse(
                heroSectionService.findAll().stream().findFirst().orElse(null),
                aboutSectionService.findAll().stream().findFirst().orElse(null),
                serviceItemService.findAll(),
                statService.findAll(),
                projectItemService.findAll(),
                testimonialService.findAll(),
                companyInfoService.findAll().stream().findFirst().orElse(null)
        );
    }
}
