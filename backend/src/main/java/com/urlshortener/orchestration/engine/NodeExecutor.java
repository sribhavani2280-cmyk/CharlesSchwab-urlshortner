package com.urlshortener.orchestration.engine;

import com.urlshortener.agent.Agent;
import com.urlshortener.agent.AgentContext;
import com.urlshortener.orchestration.domain.WorkflowNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes a node's agent with bounded retries. Deliberately free of any persistence or shared
 * mutable state so instances can run concurrently on a thread pool and synchronize via their result.
 */
public class NodeExecutor {

    private final Agent agent;

    public NodeExecutor(Agent agent) {
        this.agent = agent;
    }

    public NodeExecutionOutcome run(WorkflowNode node, AgentContext context) {
        List<NodeExecutionOutcome.AttemptRecord> attempts = new ArrayList<>();
        String lastError = null;
        for (int attempt = 1; attempt <= Math.max(1, node.getMaxAttempts()); attempt++) {
            Instant start = Instant.now();
            try {
                String output = agent.execute(context);
                long latency = Instant.now().toEpochMilli() - start.toEpochMilli();
                attempts.add(new NodeExecutionOutcome.AttemptRecord(attempt, true, latency, null));
                return new NodeExecutionOutcome(node.getNodeKey(), true, output, null, attempts);
            } catch (RuntimeException ex) {
                long latency = Instant.now().toEpochMilli() - start.toEpochMilli();
                lastError = ex.getMessage();
                attempts.add(new NodeExecutionOutcome.AttemptRecord(attempt, false, latency, lastError));
            }
        }
        return new NodeExecutionOutcome(node.getNodeKey(), false, null, lastError, attempts);
    }
}
