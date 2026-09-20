package com.example.buildpro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

// Protects everything under /admin/**, plus every write (POST/PUT/DELETE) on the
// site content endpoints (services, stats, projects, testimonials, company-info)
// and the leads read/delete endpoints, with a single admin account.
//
// Admins sign in through a real login page (/admin/login, see AdminViewController
// + static/admin/login.html) instead of the browser's native HTTP Basic prompt, so
// there's an actual session (a cookie) and a working "Log out" button - HTTP Basic
// has no real sign-out, since browsers just keep resending the cached credentials
// forever once entered.
//
// Session-cookie auth means CSRF matters in a way it didn't for stateless Basic
// auth, so CSRF protection is on for everything except the public contact form
// (visitors submitting it have no admin session to carry a token in, and it needs
// no admin auth anyway). The admin pages read the CSRF token from a readable
// cookie (XSRF-TOKEN, via CookieCsrfTokenRepository + CsrfCookieFilter forcing it
// to be issued on every response) and send it back as a header on every write -
// the standard Spring Security pattern for a JS-driven frontend rather than a
// server-rendered form (see SpaCsrfTokenRequestHandler).
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
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        // The contact form is public - visitors have no admin session to
                        // carry a CSRF token in, and submitting it needs no admin auth.
                        .ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/leads"))
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Rules are matched in order - the more specific rules must come
                        // before the broad "every GET is public" rule below, otherwise
                        // that broader match would win first and the specific ones would
                        // never be reached.
                        .requestMatchers(HttpMethod.POST, "/api/leads").permitAll()
                        .requestMatchers("/admin/login", "/admin/login.html").permitAll()
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
                // API calls (fetch from the admin pages) get a clean 401 instead of a
                // redirect when the session has expired, so the existing "not signed
                // in" handling in the admin JS keeps working; a direct browser visit
                // to a protected /admin/** page still gets redirected to the login
                // page below, which is what you want for an actual page load.
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                        )
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        // false = fall back to this URL only if there's no "saved
                        // request" (the admin page the user was actually trying to
                        // reach before being sent to log in) - so logging in from a
                        // redirect lands you back where you started, not always leads.
                        .defaultSuccessUrl("/admin/leads", false)
                        .failureUrl("/admin/login?error")
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }
}
