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

// Protects the admin leads page and the read/delete leads endpoints with a single
// admin account (HTTP Basic - the browser shows its native login prompt, no custom
// login form needed). Everything else - the public site, the combined /api/content
// endpoint, the other resource CRUD endpoints, and submitting a lead via the contact
// form - stays open, matching how the API worked before this was added.
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
                        .requestMatchers(HttpMethod.POST, "/api/leads").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/leads", "/api/leads/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/leads/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> {})
                .formLogin(form -> form.disable());
        return http.build();
    }
}
