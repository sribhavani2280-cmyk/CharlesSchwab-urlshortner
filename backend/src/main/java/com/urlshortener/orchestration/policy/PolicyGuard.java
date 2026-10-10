package com.urlshortener.orchestration.policy;

import com.urlshortener.orchestration.domain.NodeRole;
import com.urlshortener.orchestration.domain.WorkflowNode;
import com.urlshortener.orchestration.domain.WorkflowRun;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Rule-based governance layer evaluated before a node executes. Enforces three concerns the
 * assignment calls for — security, compliance, and change control — independently of the LLM:
 *
 * <ul>
 *   <li><b>Change control</b>: design and review are high-impact and require human approval.</li>
 *   <li><b>Security/compliance escalation</b>: requirements touching sensitive or destructive
 *       operations (credentials, PII, dropping/truncating data, production DB) force approval
 *       even on nodes not statically configured as gates.</li>
 *   <li><b>Hard block</b>: requirements that would weaken security controls (disable auth,
 *       remove authentication) are refused outright, triggering safe-stop.</li>
 * </ul>
 *
 * The model cannot override these rules; they are evaluated in code.
 */
@Component
public class PolicyGuard {

    private static final List<String> BLOCK_PATTERNS = List.of(
            "disable security", "disable auth", "remove auth", "remove authentication",
            "turn off authentication", "bypass authentication");

    private static final List<String> ESCALATE_PATTERNS = List.of(
            "drop table", "truncate", "delete all", "wipe", "production database", "prod db",
            "credential", "password", "secret", "api key", "pii", "personal data");

    public PolicyDecision evaluate(WorkflowRun run, WorkflowNode node) {
        String text = run.getRequirementText() == null
                ? "" : run.getRequirementText().toLowerCase(Locale.ROOT);

        for (String pattern : BLOCK_PATTERNS) {
            if (text.contains(pattern)) {
                return PolicyDecision.block("SECURITY_CONTROL",
                        "requirement would weaken a security control ('" + pattern + "')");
            }
        }
        for (String pattern : ESCALATE_PATTERNS) {
            if (text.contains(pattern)) {
                return PolicyDecision.escalate("SECURITY_SENSITIVE",
                        "sensitive/destructive operation ('" + pattern + "') requires approval");
            }
        }
        if (node.getRole() == NodeRole.DESIGN || node.getRole() == NodeRole.REVIEW) {
            return PolicyDecision.escalate("CHANGE_CONTROL",
                    "high-impact " + node.getRole() + " stage requires approval");
        }
        return PolicyDecision.allow();
    }
}
