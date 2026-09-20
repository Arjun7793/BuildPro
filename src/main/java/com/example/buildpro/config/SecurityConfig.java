package com.example.buildpro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// Protects everything under /admin/**, plus every write (POST/PUT/DELETE) on the
// site content endpoints (services, stats, projects, testimonials, company-info)
// and the leads read/delete endpoints, with a single admin account (HTTP Basic -
// the browser shows its native login prompt, no custom login form needed). Reading
// content (GET) stays public everywhere - the live site depends on it - and so does
// submitting the contact form (POST /api/leads).
//
// Credentials come from admin.username/admin.password (see application.yaml),
// backed by ADMIN_USERNAME/ADMIN_PASSWORD env vars. Local dev falls back to
// admin/changeme; prod requires both env vars to be set explicitly (see README).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless-ish admin tool protected by HTTP Basic on every request,
                // not cookie/session based auth - CSRF protection isn't needed here.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Rules are matched in order - the more specific admin-only rules
                        // must come before the broad "every GET is public" rule below,
                        // otherwise that broader match would win first and the specific
                        // ones would never be reached.
                        .requestMatchers(HttpMethod.POST, "/api/leads").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/leads", "/api/leads/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/leads/**").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/api/services", "/api/stats", "/api/projects",
                                "/api/testimonials", "/api/company-info").authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/services/**", "/api/stats/**", "/api/projects/**",
                                "/api/testimonials/**", "/api/company-info/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/services/**", "/api/stats/**", "/api/projects/**",
                                "/api/testimonials/**", "/api/company-info/**").authenticated()
                        // Everything else - every GET (the live site's own content
                        // fetch, /api/content included) and anything not matched above -
                        // stays public.
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> {})
                .formLogin(form -> form.disable());
        return http.build();
    }
}
