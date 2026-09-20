package com.example.buildpro.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Spring Security loads the CSRF token lazily by default - it's only generated
// (and its cookie written) if something actually reads it, which normally means a
// server-rendered form referencing it. We have no server-rendered forms; the
// admin pages and the login page are static HTML that read the token from a
// cookie via JS. Forcing csrfToken.getToken() here on every request resolves that
// deferred token immediately, which is what actually triggers CookieCsrfTokenRepository
// to write the XSRF-TOKEN cookie - so it's always present, from the very first
// page load, for the frontend to read. See SecurityConfig for how this is wired in.
public final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
