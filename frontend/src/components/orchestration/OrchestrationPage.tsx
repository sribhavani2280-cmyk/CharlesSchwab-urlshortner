import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../../api/client";
import { useConnection } from "../../hooks/useConnection";
import type { WorkflowRun } from "../../api/types";
import { StatusBadge } from "../StatusBadge";
import { StartRunForm } from "./StartRunForm";
import { MetricsPanel } from "./MetricsPanel";
import { RunDetail } from "./RunDetail";

export function OrchestrationPage() {
  const { connection } = useConnection();
  const [runs, setRuns] = useState<WorkflowRun[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refreshRuns = useCallback(async () => {
    setError(null);
    try {
      const list = await api.listRuns(connection);
      setRuns(list);
      setSelectedId((cur) => cur ?? list[0]?.id ?? null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not reach backend");
    }
  }, [connection]);

  useEffect(() => {
    refreshRuns();
  }, [refreshRuns]);

  return (
    <div className="grid-3">
      <div>{selectedId && <RunDetail runId={selectedId} onRunChanged={refreshRuns} />}</div>

      <div>
        <StartRunForm
          onStarted={(run) => {
            setRuns((prev) => [run, ...prev.filter((p) => p.id !== run.id)]);
            setSelectedId(run.id);
          }}
        />
        <MetricsPanel />
        <div className="panel">
          <div className="inline" style={{ justifyContent: "space-between" }}>
            <h2>Runs</h2>
            <button onClick={refreshRuns}>Refresh</button>
          </div>
          {error && <div className="error">{error}</div>}
          {runs.length === 0 ? (
            <p className="muted">No runs yet.</p>
          ) : (
            runs.map((run) => (
              <div
                key={run.id}
                className={`run-list-item ${selectedId === run.id ? "active" : ""}`}
                onClick={() => setSelectedId(run.id)}
              >
                <div className="inline" style={{ justifyContent: "space-between" }}>
                  <strong style={{ fontSize: 13 }}>{run.name}</strong>
                  <StatusBadge status={run.status} />
                </div>
                <div className="muted" style={{ fontSize: 11 }}>
                  {run.scenarioType} · {new Date(run.createdAt).toLocaleString()}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
