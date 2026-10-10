# Agentic URL Shortener — Frontend

React + TypeScript + Vite UI with two workspaces:

- **URL Shortener** — create links (with optional custom alias and expiry), list, enable/disable,
  delete, and view per-link analytics.
- **Orchestration Console** — start agentic SDLC runs, watch the live dependency graph, approve or
  reject high-impact gates, inspect each node's artifact, re-plan, rollback, resume, cancel, and
  read the audit trail and reliability metrics.

## Prerequisites

- Node.js 18+ and npm
- The backend running on `http://localhost:8080` (see `../backend/README.md`)

## Run

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Set the backend base URL and API key (`X-Api-Key`, default
`local-dev-key`) in the connection bar; both are stored in `localStorage`.

## Build

```powershell
npm run build      # typecheck + production bundle into dist/
npm run preview    # serve the built bundle
```

## Structure

```
src/
  api/        client.ts (typed fetch wrapper + API key), types.ts
  hooks/      useConnection.tsx (connection context), usePolling.ts (live refresh)
  components/
    links/          CreateLinkForm, LinksTable, LinksPage
    orchestration/  StartRunForm, RunDetail, GraphView, MetricsPanel, AuditTrail, OrchestrationPage
    ConnectionBar.tsx, StatusBadge.tsx
public/styles.css   served as a static asset (see note below)
```

## Notes

- The console polls the backend every ~2s for live run/audit updates and ~4s for metrics.
- `GraphView` derives dependency levels from each node's `dependsOn`, so parallel branches
  (testing + documentation) render on the same row and synchronize at the review gate.
- The stylesheet is loaded via `<link>` from `public/` rather than a JS `import`. This also sidesteps
  a Rollup/Vite resolution bug that triggers when the project path contains an `&` (as in an
  "AT&T" folder name). The npm scripts invoke `vite`/`tsc` through `node` for the same reason;
  on a normal clone path the standard `vite`/`tsc` shims work too.
