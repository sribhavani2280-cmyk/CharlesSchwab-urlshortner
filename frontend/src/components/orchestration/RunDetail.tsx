import { useMemo, useState } from "react";
import { api, ApiError } from "../../api/client";
import { useConnection } from "../../hooks/useConnection";
import { usePolling } from "../../hooks/usePolling";
import type { AuditEvent, WorkflowRun } from "../../api/types";
import { StatusBadge } from "../StatusBadge";
import { GraphView } from "./GraphView";
import { AuditTrail } from "./AuditTrail";

interface Props {
  runId: string;
  onRunChanged: () => void;
}

export function RunDetail({ runId, onRunChanged }: Props) {
  const { connection } = useConnection();
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [replanText, setReplanText] = useState("");

  const run = usePolling<WorkflowRun>(() => api.getRun(connection, runId), 2000, true);
  const audit = usePolling<AuditEvent[]>(() => api.audit(connection, runId), 2000, true);

  const nodes = run.data?.nodes ?? [];
  const selected = useMemo(
    () => nodes.find((n) => n.nodeKey === selectedKey) ?? null,
    [nodes, selectedKey],
  );

  async function act(fn: () => Promise<unknown>) {
    setError(null);
    try {
      await fn();
      await run.refresh();
      await audit.refresh();
      onRunChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Request failed");
    }
  }

  if (!run.data) {
    return (
      <div className="panel">
        {run.error ? <div className="error">{run.error}</div> : <p className="muted">Loading run…</p>}
      </div>
    );
  }

  const r = run.data;
  const active = r.status !== "COMPLETED" && r.status !== "CANCELLED";

  return (
    <div>
      <div className="panel">
        <div className="inline" style={{ justifyContent: "space-between" }}>
          <div>
            <h2 style={{ marginBottom: 4 }}>
              {r.name} <StatusBadge status={r.status} />
            </h2>
            <div className="muted" style={{ fontSize: 12 }}>
              {r.scenarioType} · run {r.id.slice(0, 8)} · corr {r.correlationId.slice(0, 8)}
            </div>
          </div>
          <div className="inline">
            <button onClick={() => act(() => api.resume(connection, r.id))} disabled={!active}>
              Resume
            </button>
            <button className="bad" onClick={() => act(() => api.cancel(connection, r.id))} disabled={!active}>
              Cancel
            </button>
          </div>
        </div>
        <div className="muted" style={{ marginTop: 8 }}>
          {r.requirementText}
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="grid-3">
        <div className="panel">
          <h2>Execution graph</h2>
          <GraphView nodes={nodes} selectedKey={selectedKey} onSelect={setSelectedKey} />

          <div className="field" style={{ marginTop: 16 }}>
            <label htmlFor="replan">Re-plan: change requirement (invalidates graph)</label>
            <textarea
              id="replan"
              rows={2}
              placeholder="Revised requirement…"
              value={replanText}
              onChange={(e) => setReplanText(e.target.value)}
            />
            <button
              style={{ marginTop: 8 }}
              disabled={!replanText.trim() || !active}
              onClick={() =>
                act(async () => {
                  await api.replan(connection, r.id, replanText.trim());
                  setReplanText("");
                })
              }
            >
              Re-plan
            </button>
          </div>
        </div>

        <div>
          {selected ? (
            <div className="panel">
              <h2>
                {selected.nodeKey} <StatusBadge status={selected.status} />
              </h2>
              <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>
                role {selected.role} · hash {selected.outputHash?.slice(0, 12) ?? "none"}
              </div>

              {selected.status === "AWAITING_APPROVAL" && (
                <div style={{ marginBottom: 12 }}>
                  <div className="field">
                    <label htmlFor="reason">Decision note</label>
                    <input
                      id="reason"
                      placeholder="why approved / rejected"
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                    />
                  </div>
                  <div className="inline">
                    <button
                      className="ok"
                      onClick={() =>
                        act(async () => {
                          await api.approve(connection, r.id, selected.nodeKey, true, reason, "reviewer");
                          setReason("");
                        })
                      }
                    >
                      Approve
                    </button>
                    <button
                      className="bad"
                      onClick={() =>
                        act(async () => {
                          await api.approve(connection, r.id, selected.nodeKey, false, reason, "reviewer");
                          setReason("");
                        })
                      }
                    >
                      Reject
                    </button>
                  </div>
                </div>
              )}

              {selected.status === "SUCCEEDED" && active && (
                <button
                  className="bad"
                  style={{ marginBottom: 12 }}
                  onClick={() => act(() => api.rollback(connection, r.id, selected.nodeKey))}
                >
                  Rollback this node
                </button>
              )}

              <label>Artifact output</label>
              <div className="output-box">
                {selected.output ?? <span className="muted">No output yet</span>}
              </div>
            </div>
          ) : (
            <div className="panel">
              <p className="muted">Select a node to view its artifact and approve gates.</p>
            </div>
          )}
          <AuditTrail events={audit.data ?? []} />
        </div>
      </div>
    </div>
  );
}
