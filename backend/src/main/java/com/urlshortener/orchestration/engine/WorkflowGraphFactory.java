package com.urlshortener.orchestration.engine;

import com.urlshortener.config.AppProperties;
import com.urlshortener.orchestration.domain.NodeRole;
import com.urlshortener.orchestration.domain.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the SDLC dependency graph for a run.
 *
 * <pre>
 *   requirement
 *        -> design (approval gate: plan)
 *             -> implementation
 *                  -> testing  ----\
 *                  -> documentation -> review (approval gate: release)
 * </pre>
 *
 * testing and documentation run in parallel and synchronize at review.
 */
@Component
public class WorkflowGraphFactory {

    private final AppProperties properties;

    public WorkflowGraphFactory(AppProperties properties) {
        this.properties = properties;
    }

    public List<WorkflowNode> build(String runId) {
        int maxAttempts = properties.getOrchestration().getDefaultMaxAttempts();
        List<WorkflowNode> nodes = new ArrayList<>();
        nodes.add(node(runId, "requirement", NodeRole.REQUIREMENT, Set.of(), false, maxAttempts));
        nodes.add(node(runId, "design", NodeRole.DESIGN, Set.of("requirement"), true, maxAttempts));
        nodes.add(node(runId, "implementation", NodeRole.IMPLEMENTATION, Set.of("design"), false, maxAttempts));
        nodes.add(node(runId, "testing", NodeRole.TESTING, Set.of("implementation"), false, maxAttempts));
        nodes.add(node(runId, "documentation", NodeRole.DOCUMENTATION, Set.of("implementation"), false, maxAttempts));
        nodes.add(node(runId, "review", NodeRole.REVIEW, Set.of("testing", "documentation"), true, maxAttempts));
        return nodes;
    }

    private WorkflowNode node(String runId, String key, NodeRole role, Set<String> deps,
                              boolean requiresApproval, int maxAttempts) {
        return new WorkflowNode(runId, key, role, deps, requiresApproval, maxAttempts);
    }
}
