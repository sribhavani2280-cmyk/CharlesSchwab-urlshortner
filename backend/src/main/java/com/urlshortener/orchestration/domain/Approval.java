package com.urlshortener.orchestration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Human approval record bound to the exact artifact version it approved (via artifactHash).
 * If the node output later changes, the stored hash no longer matches and the approval is stale.
 */
@Entity
@Table(name = "approval")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "node_key", nullable = false, length = 60)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalDecision decision;

    @Column(name = "artifact_hash", length = 64)
    private String artifactHash;

    @Column(length = 1000)
    private String reason;

    @Column(name = "decided_by", length = 120)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected Approval() {
    }

    public Approval(String runId, String nodeKey, ApprovalDecision decision,
                    String artifactHash, String reason, String decidedBy) {
        this.runId = runId;
        this.nodeKey = nodeKey;
        this.decision = decision;
        this.artifactHash = artifactHash;
        this.reason = reason;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public String getArtifactHash() {
        return artifactHash;
    }

    public String getReason() {
        return reason;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
