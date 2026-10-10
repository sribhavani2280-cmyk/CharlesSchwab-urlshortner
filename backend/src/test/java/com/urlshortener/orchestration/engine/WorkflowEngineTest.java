package com.urlshortener.orchestration.engine;

import com.urlshortener.agent.llm.LlmClient;
import com.urlshortener.orchestration.domain.NodeStatus;
import com.urlshortener.orchestration.domain.RunStatus;
import com.urlshortener.orchestration.domain.WorkflowNode;
import com.urlshortener.orchestration.domain.WorkflowRun;
import com.urlshortener.orchestration.repo.WorkflowNodeRepository;
import com.urlshortener.agent.AgentExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkflowEngineTest {

    @TestConfiguration
    static class FailingLlmConfig {
        /** Deterministic client that fails only when the requirement carries the FORCE_FAIL sentinel. */
        @Bean
        @Primary
        LlmClient sentinelLlmClient() {
            return new LlmClient() {
                @Override
                public String name() {
                    return "sentinel";
                }

                @Override
                public String complete(String systemPrompt, String userPrompt) {
                    if (userPrompt.contains("FORCE_FAIL")) {
                        throw new AgentExecutionException("forced failure for test");
                    }
                    return "artifact for: " + firstLine(userPrompt);
                }

                private String firstLine(String s) {
                    int i = s.indexOf('\n');
                    return i < 0 ? s : s.substring(0, i);
                }
            };
        }
    }

    @Autowired
    private WorkflowEngine engine;

    @Autowired
    private WorkflowNodeRepository nodeRepository;

    private NodeStatus status(String runId, String key) {
        return nodeRepository.findByRunIdAndNodeKey(runId, key).orElseThrow().getStatus();
    }

    @Test
    void reachesApprovalGatesAndCompletesWithGovernance() {
        WorkflowRun run = engine.startRun("greenfield", "Add custom alias support", "greenfield");

        assertThat(run.getStatus()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(status(run.getId(), "requirement")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(status(run.getId(), "design")).isEqualTo(NodeStatus.AWAITING_APPROVAL);
        assertThat(status(run.getId(), "implementation")).isEqualTo(NodeStatus.PENDING);

        engine.decideApproval(run.getId(), "design", true, "plan looks good", "lead");

        // testing and documentation ran in parallel; review now gates on them.
        assertThat(status(run.getId(), "testing")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(status(run.getId(), "documentation")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(status(run.getId(), "review")).isEqualTo(NodeStatus.AWAITING_APPROVAL);

        WorkflowRun completed = engine.decideApproval(run.getId(), "review", true, "ship it", "lead");
        assertThat(completed.getStatus()).isEqualTo(RunStatus.COMPLETED);
    }

    @Test
    void rejectionTriggersSafeStop() {
        WorkflowRun run = engine.startRun("reject", "Some requirement", "greenfield");
        WorkflowRun after = engine.decideApproval(run.getId(), "design", false, "scope unclear", "lead");

        assertThat(after.getStatus()).isEqualTo(RunStatus.FAILED);
        assertThat(status(run.getId(), "design")).isEqualTo(NodeStatus.FAILED);
        assertThat(status(run.getId(), "implementation")).isEqualTo(NodeStatus.PENDING);
    }

    @Test
    void boundedRetriesThenSafeStopOnPersistentFailure() {
        WorkflowRun run = engine.startRun("fail", "Please FORCE_FAIL this run", "brownfield");

        assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED);
        WorkflowNode requirement = nodeRepository.findByRunIdAndNodeKey(run.getId(), "requirement").orElseThrow();
        assertThat(requirement.getStatus()).isEqualTo(NodeStatus.FAILED);
        assertThat(requirement.getAttemptCount()).isEqualTo(2); // default-max-attempts
    }

    @Test
    void replanInvalidatesGraphAndReopensApproval() {
        WorkflowRun run = engine.startRun("replan", "Initial requirement", "ambiguous");
        engine.decideApproval(run.getId(), "design", true, "ok", "lead");
        engine.decideApproval(run.getId(), "review", true, "ok", "lead");
        assertThat(nodeRepository.findByRunIdAndNodeKey(run.getId(), "review").orElseThrow().getStatus())
                .isEqualTo(NodeStatus.SUCCEEDED);

        WorkflowRun replanned = engine.replanRequirement(run.getId(), "Revised, clarified requirement");

        assertThat(replanned.getStatus()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(status(run.getId(), "design")).isEqualTo(NodeStatus.AWAITING_APPROVAL);
        assertThat(status(run.getId(), "review")).isEqualTo(NodeStatus.PENDING);
    }

    @Test
    void rollbackResetsDownstreamAndHaltsForResume() {
        WorkflowRun run = engine.startRun("rollback", "Requirement", "brownfield");
        engine.decideApproval(run.getId(), "design", true, "ok", "lead");
        engine.decideApproval(run.getId(), "review", true, "ok", "lead");

        engine.rollback(run.getId(), "implementation");

        assertThat(status(run.getId(), "implementation")).isEqualTo(NodeStatus.PENDING);
        assertThat(status(run.getId(), "review")).isEqualTo(NodeStatus.PENDING);
        assertThat(status(run.getId(), "design")).isEqualTo(NodeStatus.SUCCEEDED);

        WorkflowRun resumed = engine.resume(run.getId());
        assertThat(resumed.getStatus()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(status(run.getId(), "review")).isEqualTo(NodeStatus.AWAITING_APPROVAL);
    }

    @Test
    void policyBlocksSecurityWeakeningRequirement() {
        WorkflowRun run = engine.startRun("policy", "Disable auth so redirects are open", "brownfield");

        assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED);
        assertThat(status(run.getId(), "requirement")).isEqualTo(NodeStatus.FAILED);
    }
}
