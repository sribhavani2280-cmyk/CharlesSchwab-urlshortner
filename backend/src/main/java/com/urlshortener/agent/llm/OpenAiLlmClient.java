package com.urlshortener.agent.llm;

import com.urlshortener.agent.AgentExecutionException;
import com.urlshortener.config.AppProperties;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the OpenAI Chat Completions REST API directly via Spring's RestClient.
 * Fails closed: any transport or API error surfaces as an AgentExecutionException so the
 * engine's bounded-retry and safe-stop controls take over rather than silently degrading.
 */
public class OpenAiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final String model;

    public OpenAiLlmClient(AppProperties.Llm.OpenAi config) {
        this.model = config.getModel();
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(timeoutFactory(config.getTimeoutSeconds()))
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory timeoutFactory(int seconds) {
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(seconds).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(seconds).toMillis());
        return f;
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new AgentExecutionException("OpenAI returned an empty response");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AgentExecutionException("OpenAI response had no choices");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message == null ? null : message.get("content");
            if (content == null) {
                throw new AgentExecutionException("OpenAI response had no content");
            }
            return content.toString();
        } catch (AgentExecutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AgentExecutionException("OpenAI call failed: " + ex.getMessage(), ex);
        }
    }
}
