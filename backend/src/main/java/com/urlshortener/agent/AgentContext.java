package com.urlshortener.agent;

import com.urlshortener.orchestration.domain.NodeRole;

import java.util.Map;

/** Immutable input passed to an agent for one node execution. */
public record AgentContext(
        String runId,
        String requirementText,
        String scenarioType,
        NodeRole role,
        Map<String, String> upstreamOutputs) {
}
