package com.example.buildpro.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

// Serves the admin pages at clean URLs (/admin/leads, /admin/content) instead of
// the raw static file paths. A plain forward, not a redirect - the browser's
// address bar stays on the clean URL, and since it's a forward (not a new
// request), it doesn't re-trigger Spring Security's filter chain: the original
// request already had to authenticate, per SecurityConfig's "/admin/**" rule.
@Controller
public class AdminViewController {

    @GetMapping("/admin/leads")
    public void leads(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/admin/leads.html").forward(request, response);
    }

    @GetMapping("/admin/content")
    public void content(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/admin/content.html").forward(request, response);
    }
}
