package com.urlshortener.url.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateShortLinkRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(min = 3, max = 64) String alias,
        Instant expiresAt) {
}
