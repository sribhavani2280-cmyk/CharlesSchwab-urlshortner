package com.urlshortener.agent;

import com.urlshortener.agent.llm.LlmClient;
import com.urlshortener.orchestration.domain.NodeRole;

import java.util.Map;

/**
 * Generic SDLC agent parameterized by role. Builds a role-specific prompt from the requirement
 * and upstream artifacts, delegates to the configured LLM client, and validates the output is
 * non-empty before returning it (requirement traceability is preserved by echoing the requirement).
 */
public class SdlcAgent implements Agent {

    private final NodeRole role;
    private final LlmClient llmClient;

    public SdlcAgent(NodeRole role, LlmClient llmClient) {
        this.role = role;
        this.llmClient = llmClient;
    }

    @Override
    public NodeRole role() {
        return role;
    }

    @Override
    public String execute(AgentContext context) {
        String system = systemPrompt(role);
        String user = userPrompt(context);
        String output = llmClient.complete(system, user);
        if (output == null || output.isBlank()) {
            throw new AgentExecutionException(role + " agent produced empty output");
        }
        return output;
    }

    private static String systemPrompt(NodeRole role) {
        return switch (role) {
            case REQUIREMENT -> "You are a requirements analyst. Normalize intent, flag ambiguities, "
                    + "and list acceptance criteria. Mark unclear items with NEEDS-CLARIFICATION.";
            case DESIGN -> "You are a software architect. Produce a concise design: components, APIs, "
                    + "data model, and risks. High-impact; requires human approval before implementation.";
            case IMPLEMENTATION -> "You are a senior engineer. Describe the implementation plan and the "
                    + "concrete code changes required, scoped strictly to the approved design.";
            case TESTING -> "You are a test engineer. Define unit and integration tests with pass criteria "
                    + "covering happy paths, edge cases, and failure modes.";
            case DOCUMENTATION -> "You are a technical writer. Produce API and usage documentation "
                    + "consistent with the implementation.";
            case REVIEW -> "You are a release reviewer. Assess completeness, risks, and readiness. "
                    + "High-impact; requires human approval before release.";
        };
    }

    private static String userPrompt(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Requirement:\n").append(context.requirementText()).append("\n\n");
        sb.append("Scenario: ").append(context.scenarioType()).append("\n\n");
        if (context.upstreamOutputs() != null && !context.upstreamOutputs().isEmpty()) {
            sb.append("Upstream artifacts:\n");
            for (Map.Entry<String, String> entry : context.upstreamOutputs().entrySet()) {
                sb.append("- ").append(entry.getKey()).append(":\n")
                        .append(indent(entry.getValue())).append("\n");
            }
        }
        sb.append("\nProduce the artifact for role ").append(context.role()).append('.');
        return sb.toString();
    }

    private static String indent(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.length() > 1200 ? text.substring(0, 1200) + "..." : text;
        return trimmed.lines().map(l -> "    " + l).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
