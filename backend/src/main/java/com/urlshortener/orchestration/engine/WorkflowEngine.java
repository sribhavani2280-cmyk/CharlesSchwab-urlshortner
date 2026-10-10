package com.urlshortener.orchestration.engine;

import com.urlshortener.agent.AgentContext;
import com.urlshortener.agent.AgentRegistry;
import com.urlshortener.common.ApiException;
import com.urlshortener.config.AppProperties;
import com.urlshortener.orchestration.domain.Approval;
import com.urlshortener.orchestration.domain.ApprovalDecision;
import com.urlshortener.orchestration.domain.NodeAttempt;
import com.urlshortener.orchestration.domain.NodeStatus;
import com.urlshortener.orchestration.domain.RunStatus;
import com.urlshortener.orchestration.domain.WorkflowNode;
import com.urlshortener.orchestration.domain.WorkflowRun;
import com.urlshortener.orchestration.metrics.ReliabilityMetrics;
import com.urlshortener.orchestration.policy.PolicyDecision;
import com.urlshortener.orchestration.policy.PolicyGuard;
import com.urlshortener.orchestration.repo.ApprovalRepository;
import com.urlshortener.orchestration.repo.NodeAttemptRepository;
import com.urlshortener.orchestration.repo.WorkflowNodeRepository;
import com.urlshortener.orchestration.repo.WorkflowRunRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Stateful, governed orchestration engine. Advances a run's dependency graph node-by-node,
 * executing independent ready nodes in parallel and synchronizing before dependents start.
 * Enforces post-execution approval gates on high-impact nodes, bounded retries with safe-stop,
 * re-planning on upstream changes, and rollback. All state is persisted so a run survives restart.
 */
@Service
public class WorkflowEngine {

    private final WorkflowRunRepository runRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final NodeAttemptRepository attemptRepository;
    private final ApprovalRepository approvalRepository;
    private final WorkflowGraphFactory graphFactory;
    private final AgentRegistry agentRegistry;
    private final AuditService audit;
    private final ReliabilityMetrics metrics;
    private final PolicyGuard policyGuard;
    private final AppProperties properties;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public WorkflowEngine(WorkflowRunRepository runRepository,
                          WorkflowNodeRepository nodeRepository,
                          NodeAttemptRepository attemptRepository,
                          ApprovalRepository approvalRepository,
                          WorkflowGraphFactory graphFactory,
                          AgentRegistry agentRegistry,
                          AuditService audit,
                          ReliabilityMetrics metrics,
                          PolicyGuard policyGuard,
                          AppProperties properties) {
        this.runRepository = runRepository;
        this.nodeRepository = nodeRepository;
        this.attemptRepository = attemptRepository;
        this.approvalRepository = approvalRepository;
        this.graphFactory = graphFactory;
        this.agentRegistry = agentRegistry;
        this.audit = audit;
        this.metrics = metrics;
        this.policyGuard = policyGuard;
        this.properties = properties;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    @Transactional
    public WorkflowRun startRun(String name, String requirement, String scenarioType) {
        WorkflowRun run = new WorkflowRun(name, requirement, scenarioType);
        runRepository.save(run);
        metrics.runStarted();
        audit.record(run, null, "RUN_CREATED", "Run created for scenario " + scenarioType);
        nodeRepository.saveAll(graphFactory.build(run.getId()));
        audit.record(run, null, "GRAPH_BUILT", "SDLC dependency graph instantiated");
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        return advance(run.getId());
    }

    @Transactional
    public WorkflowRun advance(String runId) {
        WorkflowRun run = loadRun(runId);
        if (run.getStatus() == RunStatus.CANCELLED) {
            return run;
        }
        List<WorkflowNode> nodes = nodeRepository.findByRunId(runId);
        Map<String, WorkflowNode> byKey = indexByKey(nodes);

        boolean progressed = true;
        while (progressed) {
            progressed = false;
            List<WorkflowNode> ready = new ArrayList<>();
            for (WorkflowNode node : nodes) {
                if (node.getStatus() == NodeStatus.PENDING && dependenciesSucceeded(node, byKey)) {
                    ready.add(node);
                }
            }
            if (ready.isEmpty()) {
                break;
            }
            executeBatch(run, ready, byKey);
            progressed = true;
        }

        finalizeRunStatus(run, nodes);
        return run;
    }

    private void executeBatch(WorkflowRun run, List<WorkflowNode> ready, Map<String, WorkflowNode> byKey) {
        // Policy gate (security / compliance / change control) evaluated before any agent runs.
        Map<String, Boolean> effectiveApproval = new LinkedHashMap<>();
        List<WorkflowNode> admitted = new ArrayList<>();
        for (WorkflowNode node : ready) {
            PolicyDecision decision = policyGuard.evaluate(run, node);
            audit.record(run, node.getNodeKey(), "POLICY_EVALUATED",
                    decision.policyId() + ": " + decision.reason());
            if (!decision.allowed()) {
                node.setStatus(NodeStatus.FAILED);
                node.setLastError("Policy " + decision.policyId() + ": " + decision.reason());
                metrics.nodeFailed();
                metrics.policyBlocked();
                audit.record(run, node.getNodeKey(), "POLICY_BLOCKED", decision.reason());
                nodeRepository.save(node);
                continue;
            }
            effectiveApproval.put(node.getNodeKey(),
                    node.isRequiresApproval() || decision.requiresApproval());
            admitted.add(node);
        }

        for (WorkflowNode node : admitted) {
            node.setStatus(NodeStatus.RUNNING);
            audit.record(run, node.getNodeKey(), "NODE_STARTED", "role=" + node.getRole());
        }

        Map<String, NodeExecutionOutcome> outcomes = new LinkedHashMap<>();
        if (properties.getOrchestration().isParallel() && admitted.size() > 1) {
            List<Future<NodeExecutionOutcome>> futures = new ArrayList<>();
            for (WorkflowNode node : admitted) {
                AgentContext ctx = buildContext(run, node, byKey);
                NodeExecutor nodeExecutor = new NodeExecutor(agentRegistry.forRole(node.getRole()));
                futures.add(executor.submit(() -> nodeExecutor.run(node, ctx)));
            }
            for (Future<NodeExecutionOutcome> future : futures) {
                NodeExecutionOutcome outcome = join(future);
                outcomes.put(outcome.nodeKey(), outcome);
            }
        } else {
            for (WorkflowNode node : admitted) {
                AgentContext ctx = buildContext(run, node, byKey);
                NodeExecutor nodeExecutor = new NodeExecutor(agentRegistry.forRole(node.getRole()));
                NodeExecutionOutcome outcome = nodeExecutor.run(node, ctx);
                outcomes.put(outcome.nodeKey(), outcome);
            }
        }

        for (WorkflowNode node : admitted) {
            applyOutcome(run, node, outcomes.get(node.getNodeKey()),
                    effectiveApproval.getOrDefault(node.getNodeKey(), node.isRequiresApproval()));
        }
    }

    private void applyOutcome(WorkflowRun run, WorkflowNode node, NodeExecutionOutcome outcome,
                              boolean requiresApproval) {
        int base = node.getAttemptCount();
        long totalLatency = 0;
        for (NodeExecutionOutcome.AttemptRecord record : outcome.attempts()) {
            NodeAttempt attempt = new NodeAttempt(node.getId(), base + record.number());
            if (record.success()) {
                attempt.succeeded();
            } else {
                attempt.failed(record.error());
            }
            attemptRepository.save(attempt);
            totalLatency += record.latencyMs();
        }
        node.incrementAttemptBy(outcome.attemptCount());
        metrics.recordAttempts(outcome.attemptCount());

        if (outcome.success()) {
            node.setOutput(outcome.output());
            node.setOutputHash(ArtifactHasher.hash(outcome.output()));
            node.setLastError(null);
            if (outcome.attemptCount() > 1) {
                metrics.recovery(totalLatency);
                audit.record(run, node.getNodeKey(), "NODE_RECOVERED",
                        "succeeded after " + outcome.attemptCount() + " attempts");
            }
            if (requiresApproval) {
                node.setStatus(NodeStatus.AWAITING_APPROVAL);
                audit.record(run, node.getNodeKey(), "NODE_AWAITING_APPROVAL",
                        "high-impact node requires human approval");
            } else {
                node.setStatus(NodeStatus.SUCCEEDED);
                audit.record(run, node.getNodeKey(), "NODE_SUCCEEDED", "artifact produced");
            }
        } else {
            node.setStatus(NodeStatus.FAILED);
            node.setLastError(outcome.error());
            metrics.nodeFailed();
            audit.record(run, node.getNodeKey(), "NODE_FAILED",
                    "exhausted " + outcome.attemptCount() + " attempts: " + outcome.error());
        }
        nodeRepository.save(node);
    }

    @Transactional
    public WorkflowRun decideApproval(String runId, String nodeKey, boolean approved, String reason, String decidedBy) {
        WorkflowRun run = loadRun(runId);
        WorkflowNode node = loadNode(runId, nodeKey);
        if (node.getStatus() != NodeStatus.AWAITING_APPROVAL) {
            throw ApiException.badRequest("Node '" + nodeKey + "' is not awaiting approval");
        }
        if (approved) {
            approvalRepository.save(new Approval(runId, nodeKey, ApprovalDecision.APPROVED,
                    node.getOutputHash(), reason, decidedBy));
            node.setStatus(NodeStatus.SUCCEEDED);
            nodeRepository.save(node);
            metrics.approvalApproved();
            audit.record(run, nodeKey, "APPROVAL_GRANTED",
                    "approved by " + decidedBy + " (hash=" + shortHash(node.getOutputHash()) + ")");
            return advance(runId);
        }
        approvalRepository.save(new Approval(runId, nodeKey, ApprovalDecision.REJECTED,
                node.getOutputHash(), reason, decidedBy));
        node.setStatus(NodeStatus.FAILED);
        node.setLastError("Rejected by " + decidedBy + ": " + reason);
        nodeRepository.save(node);
        metrics.approvalRejected();
        audit.record(run, nodeKey, "APPROVAL_REJECTED", "rejected by " + decidedBy + ": " + reason);
        return advance(runId);
    }

    @Transactional
    public WorkflowRun replanRequirement(String runId, String newRequirement) {
        WorkflowRun run = loadRun(runId);
        requireActive(run);
        run.setRequirementText(newRequirement);
        List<WorkflowNode> nodes = nodeRepository.findByRunId(runId);
        for (WorkflowNode node : nodes) {
            node.reset();
            nodeRepository.save(node);
        }
        metrics.replan();
        audit.record(run, null, "REPLAN_REQUIREMENT", "requirement changed; full graph invalidated");
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        return advance(runId);
    }

    @Transactional
    public WorkflowRun updateNodeOutput(String runId, String nodeKey, String newOutput) {
        WorkflowRun run = loadRun(runId);
        requireActive(run);
        WorkflowNode node = loadNode(runId, nodeKey);
        if (node.getStatus() != NodeStatus.SUCCEEDED && node.getStatus() != NodeStatus.AWAITING_APPROVAL) {
            throw ApiException.badRequest("Only a completed or awaiting-approval node output can be edited");
        }
        List<WorkflowNode> nodes = nodeRepository.findByRunId(runId);
        node.setOutput(newOutput);
        node.setOutputHash(ArtifactHasher.hash(newOutput));
        node.setStatus(node.isRequiresApproval() ? NodeStatus.AWAITING_APPROVAL : NodeStatus.SUCCEEDED);
        nodeRepository.save(node);

        Set<String> invalidated = invalidateDownstream(run, nodes, nodeKey);
        metrics.replan();
        audit.record(run, nodeKey, "REPLAN_OUTPUT",
                "output edited; invalidated downstream " + invalidated);
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        return advance(runId);
    }

    @Transactional
    public WorkflowRun rollback(String runId, String nodeKey) {
        WorkflowRun run = loadRun(runId);
        WorkflowNode node = loadNode(runId, nodeKey);
        List<WorkflowNode> nodes = nodeRepository.findByRunId(runId);

        Set<String> affected = new HashSet<>();
        affected.add(nodeKey);
        affected.addAll(collectDownstream(nodes, nodeKey));
        for (WorkflowNode n : nodes) {
            if (affected.contains(n.getNodeKey())) {
                n.reset();
                nodeRepository.save(n);
            }
        }
        metrics.rollback();
        audit.record(run, nodeKey, "ROLLBACK",
                "compensating rollback of " + affected + "; run halted for human resume");
        // Safe-stop: do not auto-advance. A human must explicitly resume.
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        return run;
    }

    @Transactional
    public WorkflowRun resume(String runId) {
        WorkflowRun run = loadRun(runId);
        if (run.getStatus() == RunStatus.CANCELLED) {
            throw ApiException.badRequest("Cancelled runs cannot be resumed");
        }
        audit.record(run, null, "RUN_RESUMED", "execution resumed by operator");
        return advance(runId);
    }

    @Transactional
    public WorkflowRun cancel(String runId) {
        WorkflowRun run = loadRun(runId);
        if (run.getStatus() == RunStatus.COMPLETED) {
            throw ApiException.badRequest("Completed runs cannot be cancelled");
        }
        for (WorkflowNode node : nodeRepository.findByRunId(runId)) {
            if (node.getStatus() != NodeStatus.SUCCEEDED && node.getStatus() != NodeStatus.FAILED) {
                node.setStatus(NodeStatus.CANCELLED);
                nodeRepository.save(node);
            }
        }
        run.setStatus(RunStatus.CANCELLED);
        runRepository.save(run);
        audit.record(run, null, "RUN_CANCELLED", "run cancelled by operator (safe-stop)");
        return run;
    }

    private Set<String> invalidateDownstream(WorkflowRun run, List<WorkflowNode> nodes, String changedKey) {
        Set<String> downstream = collectDownstream(nodes, changedKey);
        for (WorkflowNode node : nodes) {
            if (downstream.contains(node.getNodeKey())) {
                node.reset();
                nodeRepository.save(node);
            }
        }
        return downstream;
    }

    private Set<String> collectDownstream(List<WorkflowNode> nodes, String startKey) {
        Set<String> result = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(startKey);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (WorkflowNode node : nodes) {
                if (node.getDependsOn().contains(current) && result.add(node.getNodeKey())) {
                    queue.add(node.getNodeKey());
                }
            }
        }
        return result;
    }

    private void finalizeRunStatus(WorkflowRun run, List<WorkflowNode> nodes) {
        boolean anyFailed = nodes.stream().anyMatch(n -> n.getStatus() == NodeStatus.FAILED);
        boolean anyAwaiting = nodes.stream().anyMatch(n -> n.getStatus() == NodeStatus.AWAITING_APPROVAL);
        boolean allSucceeded = nodes.stream().allMatch(n -> n.getStatus() == NodeStatus.SUCCEEDED);
        long latency = Instant.now().toEpochMilli() - run.getCreatedAt().toEpochMilli();

        if (anyFailed) {
            if (run.getStatus() != RunStatus.FAILED) {
                run.setStatus(RunStatus.FAILED);
                metrics.runFailed(latency);
                audit.record(run, null, "RUN_FAILED", "a node failed terminally; safe-stop engaged");
            }
        } else if (anyAwaiting) {
            run.setStatus(RunStatus.AWAITING_APPROVAL);
        } else if (allSucceeded) {
            if (run.getStatus() != RunStatus.COMPLETED) {
                run.setStatus(RunStatus.COMPLETED);
                metrics.runCompleted(latency);
                audit.record(run, null, "RUN_COMPLETED", "all tracked nodes succeeded");
            }
        } else {
            run.setStatus(RunStatus.RUNNING);
        }
        run.touch();
        runRepository.save(run);
    }

    private AgentContext buildContext(WorkflowRun run, WorkflowNode node, Map<String, WorkflowNode> byKey) {
        Map<String, String> upstream = new LinkedHashMap<>();
        for (String dep : node.getDependsOn()) {
            WorkflowNode depNode = byKey.get(dep);
            if (depNode != null && depNode.getOutput() != null) {
                upstream.put(dep, depNode.getOutput());
            }
        }
        return new AgentContext(run.getId(), run.getRequirementText(), run.getScenarioType(),
                node.getRole(), upstream);
    }

    private boolean dependenciesSucceeded(WorkflowNode node, Map<String, WorkflowNode> byKey) {
        for (String dep : node.getDependsOn()) {
            WorkflowNode depNode = byKey.get(dep);
            if (depNode == null || depNode.getStatus() != NodeStatus.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }

    private Map<String, WorkflowNode> indexByKey(List<WorkflowNode> nodes) {
        Map<String, WorkflowNode> map = new LinkedHashMap<>();
        for (WorkflowNode node : nodes) {
            map.put(node.getNodeKey(), node);
        }
        return map;
    }

    private void requireActive(WorkflowRun run) {
        if (run.getStatus() == RunStatus.CANCELLED) {
            throw ApiException.badRequest("Run is cancelled");
        }
    }

    private NodeExecutionOutcome join(Future<NodeExecutionOutcome> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Node execution interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Node execution failed unexpectedly", e);
        }
    }

    private WorkflowRun loadRun(String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> ApiException.notFound("No run with id " + runId));
    }

    private WorkflowNode loadNode(String runId, String nodeKey) {
        return nodeRepository.findByRunIdAndNodeKey(runId, nodeKey)
                .orElseThrow(() -> ApiException.notFound("No node '" + nodeKey + "' in run " + runId));
    }

    private static String shortHash(String hash) {
        return hash == null ? "none" : hash.substring(0, Math.min(12, hash.length()));
    }
}
