package com.urlshortener.orchestration.engine;

import com.urlshortener.orchestration.domain.AuditEvent;
import com.urlshortener.orchestration.domain.WorkflowRun;
import com.urlshortener.orchestration.repo.AuditEventRepository;
import org.springframework.stereotype.Component;

/** Writes append-only audit records with the run's correlation id for traceability. */
@Component
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(WorkflowRun run, String nodeKey, String eventType, String detail) {
        auditEventRepository.save(new AuditEvent(
                run.getId(), run.getCorrelationId(), nodeKey, eventType, truncate(detail)));
    }

    private static String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= 2000 ? detail : detail.substring(0, 2000);
    }
}
