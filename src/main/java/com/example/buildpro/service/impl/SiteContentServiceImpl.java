package com.example.buildpro.service.impl;

import com.example.buildpro.dto.SiteContentResponse;
import com.example.buildpro.entity.ProjectItem;
import com.example.buildpro.entity.SamplePlan;
import com.example.buildpro.entity.ServiceItem;
import com.example.buildpro.entity.Stat;
import com.example.buildpro.entity.Testimonial;
import com.example.buildpro.service.AboutSectionService;
import com.example.buildpro.service.CompanyInfoService;
import com.example.buildpro.service.HeroSectionService;
import com.example.buildpro.service.ProjectItemService;
import com.example.buildpro.service.SamplePlanService;
import com.example.buildpro.service.ServiceItemService;
import com.example.buildpro.service.SiteContentService;
import com.example.buildpro.service.SiteSectionSettingsService;
import com.example.buildpro.service.StatService;
import com.example.buildpro.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final HeroSectionService heroSectionService;
    private final AboutSectionService aboutSectionService;
    private final ServiceItemService serviceItemService;
    private final StatService statService;
    private final ProjectItemService projectItemService;
    private final SamplePlanService samplePlanService;
    private final TestimonialService testimonialService;
    private final CompanyInfoService companyInfoService;
    private final SiteSectionSettingsService siteSectionSettingsService;

    @Override
    public SiteContentResponse getContent() {
        // This is the one endpoint the public page (index.html) actually fetches
        // content from, so draft items (published=false, staged via the admin
        // panel - see ServiceItem/ProjectItem/SamplePlan/Testimonial/Stat.published)
        // are filtered out here rather than at the raw /api/services, /api/stats,
        // /api/projects, /api/sample-plans, /api/testimonials endpoints themselves, which the admin
        // panel's own table view depends on returning every item, drafts
        // included, so they can be edited and republished. Hero/About/Company
        // Info are singletons with no draft concept and are unaffected.
        return new SiteContentResponse(
                heroSectionService.findAll().stream().findFirst().orElse(null),
                aboutSectionService.findAll().stream().findFirst().orElse(null),
                onlyPublished(serviceItemService.findAll(), ServiceItem::getPublished),
                onlyPublished(statService.findAll(), Stat::getPublished),
                onlyPublished(projectItemService.findAll(), ProjectItem::getPublished),
                onlyPublished(samplePlanService.findAll(), SamplePlan::getPublished),
                onlyPublished(testimonialService.findAll(), Testimonial::getPublished),
                companyInfoService.findAll().stream().findFirst().orElse(null),
                siteSectionSettingsService.findAll().stream().findFirst().orElse(null)
        );
    }

    private <T> List<T> onlyPublished(List<T> items, java.util.function.Function<T, Boolean> publishedGetter) {
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(publishedGetter.apply(item)))
                .toList();
    }
}
