package com.urlshortener.url.service;

import com.urlshortener.common.ApiException;

import java.net.URI;
import java.util.Set;

/**
 * Validates destination URLs. Rejects non-HTTP(S) schemes to avoid javascript:/file:/data: abuse.
 * Server-side fetching of destinations is intentionally out of scope (SSRF guard).
 */
public final class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_LENGTH = 2048;

    private UrlValidator() {
    }

    public static String normalizeAndValidate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw ApiException.badRequest("Destination URL must not be empty");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw ApiException.badRequest("Destination URL exceeds " + MAX_LENGTH + " characters");
        }
        final URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Destination URL is malformed");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw ApiException.badRequest("Only http and https URLs are allowed");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw ApiException.badRequest("Destination URL must include a host");
        }
        return trimmed;
    }
}
