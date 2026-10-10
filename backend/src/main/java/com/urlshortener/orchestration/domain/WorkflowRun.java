package com.urlshortener.orchestration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A single agentic SDLC execution instance for one requirement. */
@Entity
@Table(name = "workflow_run")
public class WorkflowRun {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "requirement_text", nullable = false, length = 4000)
    private String requirementText;

    @Column(name = "scenario_type", length = 40)
    private String scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkflowRun() {
    }

    public WorkflowRun(String name, String requirementText, String scenarioType) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.requirementText = requirementText;
        this.scenarioType = scenarioType;
        this.status = RunStatus.CREATED;
        this.correlationId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public void setRequirementText(String requirementText) {
        this.requirementText = requirementText;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
