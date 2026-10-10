package com.urlshortener.orchestration.web.dto;

import com.urlshortener.orchestration.domain.WorkflowNode;

import java.time.Instant;
import java.util.List;

public record NodeResponse(
        String nodeKey,
        String role,
        String status,
        List<String> dependsOn,
        boolean requiresApproval,
        int attemptCount,
        int maxAttempts,
        String outputHash,
        String output,
        String lastError,
        Instant updatedAt) {

    public static NodeResponse from(WorkflowNode node) {
        return new NodeResponse(
                node.getNodeKey(),
                node.getRole().name(),
                node.getStatus().name(),
                node.getDependsOn().stream().sorted().toList(),
                node.isRequiresApproval(),
                node.getAttemptCount(),
                node.getMaxAttempts(),
                node.getOutputHash(),
                node.getOutput(),
                node.getLastError(),
                node.getUpdatedAt());
    }
}
