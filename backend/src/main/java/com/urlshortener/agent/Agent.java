package com.urlshortener.agent;

import com.urlshortener.orchestration.domain.NodeRole;

/** A single SDLC agent. Implementations produce a text artifact for their role. */
public interface Agent {

    NodeRole role();

    /**
     * Produce this role's artifact.
     *
     * @throws AgentExecutionException when the agent cannot produce a valid artifact
     */
    String execute(AgentContext context);
}
