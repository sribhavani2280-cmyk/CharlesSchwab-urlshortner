package com.urlshortener.agent.llm;

/**
 * Deterministic, offline LLM stand-in. Produces clearly-labeled structured text so the full
 * agentic workflow runs without credentials and tests stay reproducible.
 * Output is explicitly marked as deterministic and is NOT a substitute for a real model.
 */
public class StubLlmClient implements LlmClient {

    @Override
    public String name() {
        return "stub";
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "[DETERMINISTIC STUB OUTPUT]\n"
                + "role-context: " + firstLine(systemPrompt) + "\n"
                + "--- generated artifact ---\n"
                + userPrompt.trim();
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "(none)";
        }
        int idx = text.indexOf('\n');
        return idx < 0 ? text : text.substring(0, idx);
    }
}
