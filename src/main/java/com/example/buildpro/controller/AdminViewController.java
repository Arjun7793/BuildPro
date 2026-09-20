package com.example.buildpro.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

// Serves the admin leads page at a clean URL (/admin/leads) instead of the raw
// static file path (/admin/leads.html). A plain forward, not a redirect - the
// browser's address bar stays on /admin/leads, and since it's a forward (not a
// new request), it doesn't re-trigger Spring Security's filter chain: the
// original /admin/leads request already had to authenticate, per SecurityConfig.
@Controller
public class AdminViewController {

    @GetMapping("/admin/leads")
    public void leads(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/admin/leads.html").forward(request, response);
    }
}
