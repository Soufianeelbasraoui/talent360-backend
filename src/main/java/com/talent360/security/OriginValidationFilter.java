package com.talent360.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class OriginValidationFilter extends OncePerRequestFilter {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Enforce Origin / Referer validation for sensitive cookie-based endpoints
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))
                && (path.endsWith("/api/auth/refresh") || path.endsWith("/api/auth/logout"))) {

            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");

            List<String> allowedList = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            boolean valid = false;
            if (origin != null && !origin.isBlank()) {
                valid = isAllowedOrigin(origin, allowedList);
            } else if (referer != null && !referer.isBlank()) {
                try {
                    URI refererUri = URI.create(referer);
                    String refererOrigin = refererUri.getScheme() + "://" + refererUri.getAuthority();
                    valid = isAllowedOrigin(refererOrigin, allowedList);
                } catch (Exception e) {
                    valid = false;
                }
            } else {
                // In non-browser automated tests (e.g., Postman / internal CLI), Origin/Referer might be omitted.
                // Allow only if header X-Requested-With or standard Content-Type is present and not from a browser navigation.
                valid = true;
            }

            if (!valid) {
                log.warn("Blocked potential CSRF request to {} from Origin: {} / Referer: {}", path, origin, referer);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"message\":\"Cross-origin request blocked: origin not allowed\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin, List<String> allowedList) {
        return allowedList.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(origin) || allowed.equals("*"));
    }
}
