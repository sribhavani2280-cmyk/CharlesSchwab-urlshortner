package com.urlshortener.orchestration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** One execution attempt of a node, retained for audit and retry accounting. */
@Entity
@Table(name = "node_attempt", indexes = {
        @Index(name = "ix_attempt_node", columnList = "node_id")
})
public class NodeAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, length = 36)
    private String nodeId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(length = 2000)
    private String error;

    protected NodeAttempt() {
    }

    public NodeAttempt(String nodeId, int attemptNumber) {
        this.nodeId = nodeId;
        this.attemptNumber = attemptNumber;
        this.status = AttemptStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void succeeded() {
        this.status = AttemptStatus.SUCCEEDED;
        finish();
    }

    public void failed(String error) {
        this.status = AttemptStatus.FAILED;
        this.error = error;
        finish();
    }

    private void finish() {
        this.finishedAt = Instant.now();
        this.latencyMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    public Long getId() {
        return id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getError() {
        return error;
    }
}
