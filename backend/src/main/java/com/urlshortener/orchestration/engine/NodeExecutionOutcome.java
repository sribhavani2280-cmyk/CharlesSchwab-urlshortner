package com.urlshortener.orchestration.engine;

import java.util.List;

/** Result of executing one node's agent with bounded retries. Pure data; no persistence. */
public record NodeExecutionOutcome(
        String nodeKey,
        boolean success,
        String output,
        String error,
        List<AttemptRecord> attempts) {

    public int attemptCount() {
        return attempts.size();
    }

    public record AttemptRecord(int number, boolean success, long latencyMs, String error) {
    }
}
