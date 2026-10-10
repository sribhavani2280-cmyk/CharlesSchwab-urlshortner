# Final Engineering Summary — Agentic URL Shortener

## 1. Plan and rationale

Build a runnable prototype that turns a requirement into a reviewable engineering outcome through a
**governed agentic orchestration engine**, with a URL shortener as the concrete domain. Two
components, one repository:

- **Backend** (Java 21, Spring Boot 3.3): URL shortener APIs + the orchestration engine, agents, and
  governance. H2 file database for zero-setup runnability and restart persistence.
- **Frontend** (React + TypeScript + Vite): a URL management workspace and an orchestration console
  that makes the governance visible and operable (graph, gates, audit, metrics).

Rationale: the assignment's critical differentiator is the orchestration layer, so it was given
first-class treatment (explicit dependency graph, gates, retries, rollback, re-plan, audit, metrics)
rather than a linear task chain. A deterministic agent stub lets the whole system run and be tested
without credentials; a real OpenAI client is a drop-in via configuration.

## 2. Requirement → implementation mapping

| Requirement | Where |
|---|---|
| Requirement understanding / ambiguity | `SdlcAgent` REQUIREMENT prompt (flags `NEEDS-CLARIFICATION`) |
| Task decomposition w/ dependencies | `WorkflowGraphFactory` builds the SDLC graph |
| Codebase reasoning (brownfield) | Design/implementation agents consume upstream artifacts; brownfield + rollback scenario |
| Workflow orchestration | `WorkflowEngine` — graph, gates, parallel+sync, retries, rollback, safe-stop, re-plan |
| Policy guardrails (security/compliance/change control) | `PolicyGuard` — block + approval escalation, evaluated in code before each node |
| Engineering output | Controllers, DTOs, 24 unit/integration tests, READMEs, OpenAPI-style API docs |
| Validation & risk control | `UrlValidator` (SSRF guard), rate limiting, bounded retries, safe-stop, tests |
| Controlled autonomy | Human approval gates bound to artifact SHA-256 hash; `decideApproval` |
| Final engineering summary | This document |

## 3. Architecture (control flow)

Requirement → requirement agent → **design (approval gate)** → implementation →
**testing ∥ documentation** (parallel, synchronized) → **review (approval gate)** → completed.

- Each node has entry prerequisites (all dependencies `SUCCEEDED`) and an exit artifact.
- `PolicyGuard` runs **before** every node: it can **block** (safe-stop) or **escalate to approval**,
  independently of the LLM.
- Approvals bind to the artifact's content hash; editing an upstream artifact invalidates downstream
  work and forces re-approval (dynamic re-plan).
- All state is persisted (H2 file DB), so an in-flight run survives an application restart.

## 4. Artifacts produced

- Backend service + orchestration engine + agents + policy guard (Java).
- React URL management UI and orchestration console.
- 24 automated tests (URL behavior, API integration, orchestration governance, policy rules).
- Documentation: root [README](../README.md), [backend/README](../backend/README.md),
  [frontend/README](../frontend/README.md), architecture + sequence diagrams, UI screenshots.

## 5. Validation approach

- **Unit**: code generation, URL validation, short-link service (collisions, expiry, redirect),
  policy rules.
- **Integration** (`@SpringBootTest` + MockMvc): URL API (create/redirect/analytics/auth) and
  orchestration API (start → approve → complete, auth).
- **Engine governance**: approval gates, parallel branches, bounded retries + safe-stop, re-plan,
  rollback + resume, policy block.
- **Runtime**: live smoke of both apps; the console was exercised end-to-end in a browser.

## 6. Risks, trade-offs, and failure handling

| Risk / failure | Mitigation |
|---|---|
| Agent produces a bad/invalid artifact | Bounded retries, then terminal failure + safe-stop |
| Upstream requirement changes mid-run | Re-plan invalidates affected downstream + stale approvals |
| High-impact or unsafe action | `PolicyGuard` blocks or forces human approval |
| Destructive operation can't be auto-undone | Rollback covers graph state; DB-destructive ops need a compensation plan (documented) |
| Model/repository content is untrusted | Permissions enforced in code (API key, URL scheme allowlist), not by the LLM |
| Short-code collision under concurrency | DB unique constraint + bounded regeneration retries |

Key trade-offs: H2 + `ddl-auto` (fast setup) over Flyway/PostgreSQL (production migrations);
in-process metrics/rate-limit (simple) over a shared store (scale); agents produce governed text
artifacts (safe, demoable) over editing a sandboxed repo (needs container isolation).

## 7. Assumptions

- Single-node local execution; one reviewer acting as the human approver.
- Redirects are intentionally public; management/analytics/orchestration APIs require an API key.
- The deterministic stub is acceptable for demos and tests; a real model is enabled by configuration.

## 8. Limitations

- Agents do not edit a separate sandboxed repository (no container isolation here).
- Brownfield reasoning is demonstrated via agent artifacts, not full static analysis of a target repo.
- Metrics and rate limiting are process-local; the audit trail is append-only in the same database,
  not a tamper-evident external store.
- Not load/performance tested; this is a production-minded prototype, not a certified platform.
