package com.example.buildpro.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BuildPro API",
                version = "0.0.1-SNAPSHOT",
                description = "APIs backing the BuildPro Construction page: hero/about section content, services, "
                        + "stats, projects, testimonials, company info and contact-form leads, all stored in Postgres."
        )
)
public class OpenApiConfig {
}
