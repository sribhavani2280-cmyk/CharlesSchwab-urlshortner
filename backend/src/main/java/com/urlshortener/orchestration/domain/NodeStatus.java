package com.urlshortener.orchestration.domain;

public enum NodeStatus {
    PENDING,
    AWAITING_APPROVAL,
    RUNNING,
    SUCCEEDED,
    FAILED,
    ROLLED_BACK,
    CANCELLED
}
