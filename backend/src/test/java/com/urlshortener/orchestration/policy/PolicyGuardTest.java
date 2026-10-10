package com.urlshortener.orchestration.policy;

import com.urlshortener.orchestration.domain.NodeRole;
import com.urlshortener.orchestration.domain.WorkflowNode;
import com.urlshortener.orchestration.domain.WorkflowRun;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyGuardTest {

    private final PolicyGuard guard = new PolicyGuard();

    private WorkflowNode node(NodeRole role) {
        return new WorkflowNode("run", role.name().toLowerCase(), role, Set.of(), false, 2);
    }

    private WorkflowRun run(String requirement) {
        return new WorkflowRun("n", requirement, "greenfield");
    }

    @Test
    void allowsOrdinaryImplementationNode() {
        PolicyDecision d = guard.evaluate(run("Add custom alias support"), node(NodeRole.IMPLEMENTATION));
        assertThat(d.allowed()).isTrue();
        assertThat(d.requiresApproval()).isFalse();
    }

    @Test
    void escalatesDesignAndReviewForChangeControl() {
        assertThat(guard.evaluate(run("x"), node(NodeRole.DESIGN)).requiresApproval()).isTrue();
        assertThat(guard.evaluate(run("x"), node(NodeRole.REVIEW)).requiresApproval()).isTrue();
    }

    @Test
    void escalatesSensitiveRequirementOnAnyNode() {
        PolicyDecision d = guard.evaluate(
                run("Migrate and DROP TABLE short_link in production database"),
                node(NodeRole.IMPLEMENTATION));
        assertThat(d.allowed()).isTrue();
        assertThat(d.requiresApproval()).isTrue();
        assertThat(d.policyId()).isEqualTo("SECURITY_SENSITIVE");
    }

    @Test
    void blocksRequirementThatWeakensSecurity() {
        PolicyDecision d = guard.evaluate(
                run("Disable auth so redirects are open"), node(NodeRole.IMPLEMENTATION));
        assertThat(d.allowed()).isFalse();
        assertThat(d.policyId()).isEqualTo("SECURITY_CONTROL");
    }
}
