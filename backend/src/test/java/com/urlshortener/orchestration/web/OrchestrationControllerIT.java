package com.urlshortener.orchestration.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrchestrationControllerIT {

    private static final String API_KEY = "test-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startsRunAndDrivesApprovalToCompletion() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "greenfield demo",
                "requirement", "Support branded short links",
                "scenarioType", "greenfield"));

        String response = mockMvc.perform(post("/api/v1/orchestration/runs")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        String runId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/v1/orchestration/runs/" + runId + "/nodes/design/approve")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", true, "decidedBy", "lead"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orchestration/runs/" + runId + "/nodes/review/approve")
                        .header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", true, "decidedBy", "lead"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/orchestration/runs/" + runId + "/audit").header("X-Api-Key", API_KEY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orchestration/metrics").header("X-Api-Key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runsCompleted").exists());
    }

    @Test
    void requiresApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/orchestration/runs"))
                .andExpect(status().isUnauthorized());
    }
}
