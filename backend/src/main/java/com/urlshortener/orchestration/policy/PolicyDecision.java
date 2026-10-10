package com.urlshortener.orchestration.policy;

/** Outcome of a policy evaluation for one node. */
public record PolicyDecision(
        boolean allowed,
        boolean requiresApproval,
        String policyId,
        String reason) {

    public static PolicyDecision allow() {
        return new PolicyDecision(true, false, "NONE", "no policy triggered");
    }

    public static PolicyDecision escalate(String policyId, String reason) {
        return new PolicyDecision(true, true, policyId, reason);
    }

    public static PolicyDecision block(String policyId, String reason) {
        return new PolicyDecision(false, false, policyId, reason);
    }
}
