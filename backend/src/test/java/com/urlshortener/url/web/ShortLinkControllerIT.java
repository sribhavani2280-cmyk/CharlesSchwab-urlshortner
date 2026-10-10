package com.urlshortener.url.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShortLinkControllerIT {

    private static final String API_KEY = "test-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsResolvesAndDisablesLink() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("url", "https://example.com/landing"));

        String response = mockMvc.perform(post("/api/v1/links")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.shortUrl", containsString("/")))
                .andReturn().getResponse().getContentAsString();

        String code = objectMapper.readTree(response).get("code").asText();

        // Public redirect works without an API key.
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/landing"));

        // Disable, then redirect should 404.
        mockMvc.perform(post("/api/v1/links/" + code + "/disable").header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void honorsCustomAliasAndRejectsDuplicate() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("url", "https://example.com", "alias", "launch-2026"));

        mockMvc.perform(post("/api/v1/links")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("launch-2026"));

        mockMvc.perform(post("/api/v1/links")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsNonHttpScheme() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("url", "ftp://example.com"));
        mockMvc.perform(post("/api/v1/links")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresApiKeyForManagementApi() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("url", "https://example.com"));
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAnalyticsAfterClicks() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("url", "https://example.com/metrics"));
        String response = mockMvc.perform(post("/api/v1/links")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(response).get("code").asText();

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/v1/links/" + code + "/analytics").header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(2));
    }
}
