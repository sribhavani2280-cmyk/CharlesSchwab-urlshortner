package com.urlshortener.orchestration.web.dto;

import com.urlshortener.orchestration.domain.WorkflowNode;
import com.urlshortener.orchestration.domain.WorkflowRun;

import java.time.Instant;
import java.util.List;

public record RunResponse(
        String id,
        String name,
        String scenarioType,
        String status,
        String requirementText,
        String correlationId,
        Instant createdAt,
        Instant updatedAt,
        List<NodeResponse> nodes) {

    public static RunResponse of(WorkflowRun run, List<WorkflowNode> nodes) {
        return new RunResponse(
                run.getId(),
                run.getName(),
                run.getScenarioType(),
                run.getStatus().name(),
                run.getRequirementText(),
                run.getCorrelationId(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                nodes.stream().map(NodeResponse::from).toList());
    }

    public static RunResponse summary(WorkflowRun run) {
        return new RunResponse(
                run.getId(),
                run.getName(),
                run.getScenarioType(),
                run.getStatus().name(),
                run.getRequirementText(),
                run.getCorrelationId(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                List.of());
    }
}
