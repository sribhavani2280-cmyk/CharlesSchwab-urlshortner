# Agentic URL Shortener — Backend

A URL shortener plus a **governed agentic SDLC orchestration engine**, built with Java 21 and
Spring Boot. The orchestration layer is the core differentiator: it coordinates requirements →
design → implementation → testing/documentation → review as an explicit dependency graph with
entry/exit gates, parallel branches, human approval checkpoints, bounded retries, re-planning,
rollback, safe-stop, audit-grade traceability, and reliability metrics.

Agents execute under defined autonomy boundaries; humans own approvals and final quality control.

## Tech stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Validation, Actuator)
- H2 (file-based) — zero external infrastructure, survives restart (demonstrates run recovery)
- JUnit 5, Mockito, Spring MockMvc

## Prerequisites

- JDK 21 on `PATH`
- Maven 3.8+
- No Docker or database server required

## Run

```powershell
cd backend
mvn spring-boot:run
```

App starts on `http://localhost:8080`. The H2 file database is created under `backend/data/`.

### Configuration (env vars)

| Variable | Default | Purpose |
|---|---|---|
| `APP_SECURITY_API_KEY` | `local-dev-key` | API key for management/analytics/orchestration APIs |
| `APP_LLM_PROVIDER` | `stub` | `stub` (deterministic, offline) or `openai` |
| `OPENAI_API_KEY` | _(empty)_ | Required when provider is `openai`. Supply via environment only |

With `APP_LLM_PROVIDER=openai` and no key, the app logs a warning and falls back to the
deterministic stub so it always runs. The stub clearly labels its output and is not a substitute
for a real model.

## Test

```powershell
cd backend
mvn test
```

19 tests cover URL behavior (collisions, aliases, expiry, redirect, analytics, auth) and
orchestration governance (approval gates, parallel branches, bounded retries + safe-stop,
re-planning, rollback + resume).

## Security model

- Redirects (`GET /{code}`) are public — the shortener's primary surface.
- All `/api/**` endpoints require the `X-Api-Key` header.
- Destination URLs are validated to `http`/`https` only (blocks `javascript:`/`data:`/`file:`).
  Server-side fetching of destinations is intentionally excluded (SSRF guard).
- Client IPs are stored as a salted SHA-256 prefix, not in the clear.

## URL API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/links` | key | Create link. Body: `{ "url", "alias?", "expiresAt?" }` |
| GET | `/api/v1/links/{code}` | key | Fetch link metadata |
| GET | `/api/v1/links` | key | List links (paged) |
| POST | `/api/v1/links/{code}/disable` | key | Disable a link |
| POST | `/api/v1/links/{code}/enable` | key | Re-enable a link |
| DELETE | `/api/v1/links/{code}` | key | Delete a link |
| GET | `/api/v1/links/{code}/analytics?windowDays=30` | key | Click totals + by-day + top referers |
| GET | `/{code}` | public | 302 redirect; records a click |

## Orchestration API

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/orchestration/runs` | Start a run. Body: `{ "name", "requirement", "scenarioType?" }` |
| GET | `/api/v1/orchestration/runs` | List runs |
| GET | `/api/v1/orchestration/runs/{id}` | Run detail with node graph |
| POST | `/api/v1/orchestration/runs/{id}/nodes/{nodeKey}/approve` | Approve/reject a gate. Body: `{ "approved", "reason?", "decidedBy?" }` |
| POST | `/api/v1/orchestration/runs/{id}/replan` | Re-plan on requirement change. Body: `{ "requirement" }` |
| POST | `/api/v1/orchestration/runs/{id}/nodes/{nodeKey}/output` | Edit an artifact; invalidates downstream. Body: `{ "output" }` |
| POST | `/api/v1/orchestration/runs/{id}/nodes/{nodeKey}/rollback` | Roll back a node + dependents (safe-stop) |
| POST | `/api/v1/orchestration/runs/{id}/resume` | Resume after rollback |
| POST | `/api/v1/orchestration/runs/{id}/cancel` | Cancel a run (safe-stop) |
| GET | `/api/v1/orchestration/runs/{id}/audit` | Append-only audit trail |
| GET | `/api/v1/orchestration/runs/{id}/approvals` | Approval records (bound to artifact hash) |
| GET | `/api/v1/orchestration/metrics` | Reliability metrics |

## Orchestration model

```
requirement
   └─> design  (approval gate: plan)
          └─> implementation
                 ├─> testing        ─┐  (parallel)
                 └─> documentation  ─┴─> review  (approval gate: release)
```

- **Gates**: `design` and `review` are high-impact. The agent produces the artifact, then the node
  enters `AWAITING_APPROVAL`; a human must approve before dependents run.
- **Parallel + sync**: `testing` and `documentation` run concurrently and synchronize at `review`.
- **Approval binds to the artifact**: each approval stores the SHA-256 hash of the exact output it
  approved. Editing an upstream artifact invalidates downstream nodes and forces re-approval.
- **Bounded retries + safe-stop**: a failing agent is retried up to `default-max-attempts`; on
  exhaustion the node fails terminally and the run safe-stops (no dependents run).
- **Re-plan**: changing the requirement resets the whole graph; editing one node resets only its
  transitive dependents.
- **Rollback**: resets a node and its dependents and halts for an explicit human `resume`.
- **Recovery**: all state is persisted, so an in-flight run survives an application restart.
- **Audit + metrics**: every transition writes a correlated audit event; metrics expose success
  rate, retry/rollback frequency, MTTR, and average end-to-end latency.

## Three scenarios

- **Greenfield** — `requirement`: "Add custom alias support". Runs the full graph; approve `design`
  then `review` to completion.
- **Brownfield** — start a run, then `rollback` the `implementation` node and `resume` to show
  controlled change management against existing work.
- **Ambiguous** — "Support branded links and better analytics". The requirement agent flags
  `NEEDS-CLARIFICATION`; use `replan` with a clarified requirement to re-drive the graph under
  governance.

## Limitations (prototype scope)

- Single-node: rate limiter and reliability metrics are in-process; a distributed deployment would
  use Redis/a shared store.
- Agents produce engineering artifacts (text) under governance; they do not edit a separate
  sandboxed repository (that would require container isolation, out of scope here).
- H2 + `ddl-auto=update` for zero-setup runnability; a production build would use Flyway migrations
  on PostgreSQL.
- Audit trail is append-only in the same database, not a tamper-evident external store.
