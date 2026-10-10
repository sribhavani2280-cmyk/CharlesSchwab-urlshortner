package com.urlshortener.orchestration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartRunRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 4000) String requirement,
        @Size(max = 40) String scenarioType) {
}
