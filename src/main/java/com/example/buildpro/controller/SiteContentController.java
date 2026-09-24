package com.example.buildpro.controller;

import com.example.buildpro.dto.SiteContentResponse;
import com.example.buildpro.service.SiteContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// One combined call that returns everything the page needs to render itself -
// used by the static frontend instead of five separate requests.
@RestController
@RequestMapping(value = "/api/content", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Site Content", description = "Combined content for populating the static page in one call")
public class SiteContentController {

    private final SiteContentService siteContentService;

    @GetMapping
    @Operation(summary = "Get services, stats, projects, sample plans, testimonials, company info and section visibility settings together")
    public SiteContentResponse getContent() {
        return siteContentService.getContent();
    }
}
