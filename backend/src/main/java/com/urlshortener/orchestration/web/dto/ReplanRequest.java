package com.urlshortener.orchestration.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplanRequest(
        @NotBlank String requirement) {
}
