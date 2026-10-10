package com.urlshortener.agent;

import com.urlshortener.agent.llm.LlmClient;
import com.urlshortener.orchestration.domain.NodeRole;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Resolves the agent responsible for a given SDLC role. */
@Component
public class AgentRegistry {

    private final Map<NodeRole, Agent> agents = new EnumMap<>(NodeRole.class);

    public AgentRegistry(LlmClient llmClient) {
        for (NodeRole role : NodeRole.values()) {
            agents.put(role, new SdlcAgent(role, llmClient));
        }
    }

    public Agent forRole(NodeRole role) {
        Agent agent = agents.get(role);
        if (agent == null) {
            throw new IllegalStateException("No agent registered for role " + role);
        }
        return agent;
    }
}
