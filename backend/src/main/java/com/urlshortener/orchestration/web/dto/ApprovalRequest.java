package com.urlshortener.orchestration.web.dto;

import jakarta.validation.constraints.NotNull;

public record ApprovalRequest(
        @NotNull Boolean approved,
        String reason,
        String decidedBy) {
}
