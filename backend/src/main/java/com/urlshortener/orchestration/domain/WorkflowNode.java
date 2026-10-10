package com.urlshortener.orchestration.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A node in a run's dependency graph, owned by one SDLC role. */
@Entity
@Table(name = "workflow_node", indexes = {
        @Index(name = "ix_node_run", columnList = "run_id")
})
public class WorkflowNode {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "node_key", nullable = false, length = 60)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NodeRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NodeStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_node_dependency",
            joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "depends_on_key", length = 60)
    private Set<String> dependsOn = new HashSet<>();

    /** High-impact nodes require explicit human approval before they execute. */
    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Lob
    @Column(name = "output")
    private String output;

    @Column(name = "output_hash", length = 64)
    private String outputHash;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkflowNode() {
    }

    public WorkflowNode(String runId, String nodeKey, NodeRole role, Set<String> dependsOn,
                        boolean requiresApproval, int maxAttempts) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.nodeKey = nodeKey;
        this.role = role;
        this.dependsOn = new HashSet<>(dependsOn);
        this.requiresApproval = requiresApproval;
        this.maxAttempts = maxAttempts;
        this.status = NodeStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void reset() {
        this.status = NodeStatus.PENDING;
        this.output = null;
        this.outputHash = null;
        this.lastError = null;
        this.attemptCount = 0;
        touch();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public NodeRole getRole() {
        return role;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
        touch();
    }

    public Set<String> getDependsOn() {
        return dependsOn;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttempt() {
        this.attemptCount++;
        touch();
    }

    public void incrementAttemptBy(int delta) {
        this.attemptCount += delta;
        touch();
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
        touch();
    }

    public String getOutputHash() {
        return outputHash;
    }

    public void setOutputHash(String outputHash) {
        this.outputHash = outputHash;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
