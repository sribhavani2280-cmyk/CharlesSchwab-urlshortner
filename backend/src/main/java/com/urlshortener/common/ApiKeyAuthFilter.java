package com.urlshortener.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces an API key on management/analytics/orchestration endpoints.
 * Public redirects, health checks and the dev H2 console are intentionally exempt.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";

    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // let CORS preflight through
        }
        String path = request.getServletPath();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        String expected = properties.getSecurity().getApiKey();
        if (expected != null && !expected.isBlank() && expected.equals(provided)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Missing or invalid " + HEADER + " header"));
    }
}
