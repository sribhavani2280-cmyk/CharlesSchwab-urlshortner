package com.urlshortener.agent;

/** Raised when an agent fails to produce a valid artifact; drives bounded retry in the engine. */
public class AgentExecutionException extends RuntimeException {

    public AgentExecutionException(String message) {
        super(message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
