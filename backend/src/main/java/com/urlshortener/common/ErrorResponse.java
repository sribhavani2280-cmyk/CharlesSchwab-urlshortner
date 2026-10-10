package com.urlshortener.common;

import java.time.Instant;
import java.util.Map;

/** Consistent error body returned by every API endpoint. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(Instant.now(), status, code, message, Map.of());
    }

    public static ErrorResponse of(int status, String code, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, fieldErrors);
    }
}
