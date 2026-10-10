package com.urlshortener.orchestration.web;

import com.urlshortener.orchestration.domain.Approval;
import com.urlshortener.orchestration.domain.AuditEvent;
import com.urlshortener.orchestration.domain.WorkflowNode;
import com.urlshortener.orchestration.domain.WorkflowRun;
import com.urlshortener.orchestration.engine.WorkflowEngine;
import com.urlshortener.orchestration.metrics.ReliabilityMetrics;
import com.urlshortener.orchestration.repo.ApprovalRepository;
import com.urlshortener.orchestration.repo.AuditEventRepository;
import com.urlshortener.orchestration.repo.WorkflowNodeRepository;
import com.urlshortener.orchestration.repo.WorkflowRunRepository;
import com.urlshortener.orchestration.web.dto.ApprovalRequest;
import com.urlshortener.orchestration.web.dto.ReplanRequest;
import com.urlshortener.orchestration.web.dto.RunResponse;
import com.urlshortener.orchestration.web.dto.StartRunRequest;
import com.urlshortener.orchestration.web.dto.UpdateOutputRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orchestration")
public class OrchestrationController {

    private final WorkflowEngine engine;
    private final WorkflowRunRepository runRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditEventRepository auditEventRepository;
    private final ReliabilityMetrics metrics;

    public OrchestrationController(WorkflowEngine engine,
                                   WorkflowRunRepository runRepository,
                                   WorkflowNodeRepository nodeRepository,
                                   ApprovalRepository approvalRepository,
                                   AuditEventRepository auditEventRepository,
                                   ReliabilityMetrics metrics) {
        this.engine = engine;
        this.runRepository = runRepository;
        this.nodeRepository = nodeRepository;
        this.approvalRepository = approvalRepository;
        this.auditEventRepository = auditEventRepository;
        this.metrics = metrics;
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse start(@Valid @RequestBody StartRunRequest request) {
        String scenario = request.scenarioType() == null ? "greenfield" : request.scenarioType();
        WorkflowRun run = engine.startRun(request.name(), request.requirement(), scenario);
        return detail(run.getId());
    }

    @GetMapping("/runs")
    public List<RunResponse> list() {
        return runRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RunResponse::summary)
                .toList();
    }

    @GetMapping("/runs/{id}")
    public RunResponse detail(@PathVariable String id) {
        WorkflowRun run = runRepository.findById(id)
                .orElseThrow(() -> com.urlshortener.common.ApiException.notFound("No run with id " + id));
        List<WorkflowNode> nodes = nodeRepository.findByRunId(id);
        return RunResponse.of(run, nodes);
    }

    @PostMapping("/runs/{id}/nodes/{nodeKey}/approve")
    public RunResponse approve(@PathVariable String id, @PathVariable String nodeKey,
                               @Valid @RequestBody ApprovalRequest request) {
        String decidedBy = request.decidedBy() == null ? "operator" : request.decidedBy();
        engine.decideApproval(id, nodeKey, request.approved(), request.reason(), decidedBy);
        return detail(id);
    }

    @PostMapping("/runs/{id}/replan")
    public RunResponse replan(@PathVariable String id, @Valid @RequestBody ReplanRequest request) {
        engine.replanRequirement(id, request.requirement());
        return detail(id);
    }

    @PostMapping("/runs/{id}/nodes/{nodeKey}/output")
    public RunResponse updateOutput(@PathVariable String id, @PathVariable String nodeKey,
                                    @Valid @RequestBody UpdateOutputRequest request) {
        engine.updateNodeOutput(id, nodeKey, request.output());
        return detail(id);
    }

    @PostMapping("/runs/{id}/nodes/{nodeKey}/rollback")
    public RunResponse rollback(@PathVariable String id, @PathVariable String nodeKey) {
        engine.rollback(id, nodeKey);
        return detail(id);
    }

    @PostMapping("/runs/{id}/resume")
    public RunResponse resume(@PathVariable String id) {
        engine.resume(id);
        return detail(id);
    }

    @PostMapping("/runs/{id}/cancel")
    public RunResponse cancel(@PathVariable String id) {
        engine.cancel(id);
        return detail(id);
    }

    @GetMapping("/runs/{id}/audit")
    public List<AuditEvent> audit(@PathVariable String id) {
        return auditEventRepository.findByRunIdOrderByCreatedAtAsc(id);
    }

    @GetMapping("/runs/{id}/approvals")
    public List<Approval> approvals(@PathVariable String id) {
        return approvalRepository.findByRunIdOrderByDecidedAtAsc(id);
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return metrics.snapshot();
    }
}
