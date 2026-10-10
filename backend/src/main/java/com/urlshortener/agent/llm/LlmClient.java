package com.urlshortener.agent.llm;

/** Abstraction over an LLM text-completion provider. */
public interface LlmClient {

    String name();

    /**
     * @throws com.urlshortener.agent.AgentExecutionException if the provider call fails
     */
    String complete(String systemPrompt, String userPrompt);
}
