package com.example.buildpro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// A couple of small, non-sensitive front-end display settings, read from config
// instead of hardcoded in the static pages - currently just the timezone the
// admin leads page formats submission times in. Public: nothing here is secret,
// and the admin page itself is what's actually protected (see SecurityConfig).
@RestController
@RequestMapping(value = "/api/config", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Config", description = "Public front-end display configuration")
public class AppConfigController {

    @Value("${app.display-timezone}")
    private String displayTimezone;

    @GetMapping
    @Operation(summary = "Front-end display settings (e.g. the timezone the admin page shows dates in)")
    public Map<String, String> getConfig() {
        return Map.of("displayTimezone", displayTimezone);
    }
}
