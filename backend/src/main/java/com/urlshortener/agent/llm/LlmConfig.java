package com.urlshortener.agent.llm;

import com.urlshortener.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmClient llmClient(AppProperties properties) {
        AppProperties.Llm llm = properties.getLlm();
        if ("openai".equalsIgnoreCase(llm.getProvider())) {
            String key = llm.getOpenai().getApiKey();
            if (key != null && !key.isBlank()) {
                log.info("LLM provider: openai (model={})", llm.getOpenai().getModel());
                return new OpenAiLlmClient(llm.getOpenai());
            }
            log.warn("LLM provider 'openai' selected but OPENAI_API_KEY is empty; "
                    + "falling back to deterministic stub. Set OPENAI_API_KEY for live model calls.");
        }
        log.info("LLM provider: stub (deterministic, offline)");
        return new StubLlmClient();
    }
}
