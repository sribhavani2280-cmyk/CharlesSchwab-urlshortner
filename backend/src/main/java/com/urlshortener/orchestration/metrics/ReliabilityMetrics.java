package com.urlshortener.orchestration.metrics;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory reliability counters for the orchestration engine. Prototype-grade: process-local and
 * reset on restart. Durable metrics storage is a documented scaling step.
 */
@Component
public class ReliabilityMetrics {

    private final AtomicLong runsStarted = new AtomicLong();
    private final AtomicLong runsCompleted = new AtomicLong();
    private final AtomicLong runsFailed = new AtomicLong();
    private final AtomicLong nodeAttempts = new AtomicLong();
    private final AtomicLong nodeRetries = new AtomicLong();
    private final AtomicLong nodeFailures = new AtomicLong();
    private final AtomicLong rollbacks = new AtomicLong();
    private final AtomicLong replans = new AtomicLong();
    private final AtomicLong policyBlocks = new AtomicLong();
    private final AtomicLong approvalsApproved = new AtomicLong();
    private final AtomicLong approvalsRejected = new AtomicLong();
    private final AtomicLong totalRunLatencyMs = new AtomicLong();
    private final AtomicLong totalRecoveryMs = new AtomicLong();
    private final AtomicLong recoveries = new AtomicLong();

    public void runStarted() {
        runsStarted.incrementAndGet();
    }

    public void runCompleted(long latencyMs) {
        runsCompleted.incrementAndGet();
        totalRunLatencyMs.addAndGet(latencyMs);
    }

    public void runFailed(long latencyMs) {
        runsFailed.incrementAndGet();
        totalRunLatencyMs.addAndGet(latencyMs);
    }

    public void recordAttempts(int attempts) {
        nodeAttempts.addAndGet(attempts);
        if (attempts > 1) {
            nodeRetries.addAndGet(attempts - 1L);
        }
    }

    public void nodeFailed() {
        nodeFailures.incrementAndGet();
    }

    public void recovery(long recoveryMs) {
        recoveries.incrementAndGet();
        totalRecoveryMs.addAndGet(recoveryMs);
    }

    public void rollback() {
        rollbacks.incrementAndGet();
    }

    public void replan() {
        replans.incrementAndGet();
    }

    public void policyBlocked() {
        policyBlocks.incrementAndGet();
    }

    public void approvalApproved() {
        approvalsApproved.incrementAndGet();
    }

    public void approvalRejected() {
        approvalsRejected.incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        long attempts = nodeAttempts.get();
        long failures = nodeFailures.get();
        long completedRuns = runsCompleted.get();
        long totalRuns = completedRuns + runsFailed.get();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runsStarted", runsStarted.get());
        m.put("runsCompleted", completedRuns);
        m.put("runsFailed", runsFailed.get());
        m.put("nodeAttempts", attempts);
        m.put("policyBlocks", policyBlocks.get());
        m.put("nodeRetries", nodeRetries.get());
        m.put("nodeFailures", failures);
        m.put("rollbacks", rollbacks.get());
        m.put("replans", replans.get());
        m.put("approvalsApproved", approvalsApproved.get());
        m.put("approvalsRejected", approvalsRejected.get());
        m.put("nodeSuccessRate", attempts == 0 ? 1.0 : round((attempts - failures) / (double) attempts));
        m.put("retryFrequency", attempts == 0 ? 0.0 : round(nodeRetries.get() / (double) attempts));
        m.put("runSuccessRate", totalRuns == 0 ? 1.0 : round(completedRuns / (double) totalRuns));
        m.put("avgRunLatencyMs", totalRuns == 0 ? 0 : totalRunLatencyMs.get() / totalRuns);
        m.put("mttrMs", recoveries.get() == 0 ? 0 : totalRecoveryMs.get() / recoveries.get());
        return m;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
