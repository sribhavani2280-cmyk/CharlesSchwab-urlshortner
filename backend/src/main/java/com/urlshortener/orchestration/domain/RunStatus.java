package com.urlshortener.orchestration.domain;

public enum RunStatus {
    CREATED,
    RUNNING,
    AWAITING_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED
}
