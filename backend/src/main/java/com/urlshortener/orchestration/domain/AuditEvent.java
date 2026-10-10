package com.urlshortener.orchestration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Append-only audit record providing decision lineage and traceability for a run. */
@Entity
@Table(name = "audit_event", indexes = {
        @Index(name = "ix_audit_run", columnList = "run_id, created_at")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    @Column(name = "node_key", length = 60)
    private String nodeKey;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(String runId, String correlationId, String nodeKey, String eventType, String detail) {
        this.runId = runId;
        this.correlationId = correlationId;
        this.nodeKey = nodeKey;
        this.eventType = eventType;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
