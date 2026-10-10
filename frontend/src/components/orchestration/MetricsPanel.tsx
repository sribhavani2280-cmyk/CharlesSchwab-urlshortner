import { useConnection } from "../../hooks/useConnection";
import { usePolling } from "../../hooks/usePolling";
import { api } from "../../api/client";
import type { Metrics } from "../../api/types";

const FIELDS: { key: keyof Metrics; label: string; fmt?: (v: number) => string }[] = [
  { key: "runSuccessRate", label: "Run success", fmt: (v) => `${Math.round(v * 100)}%` },
  { key: "nodeSuccessRate", label: "Node success", fmt: (v) => `${Math.round(v * 100)}%` },
  { key: "retryFrequency", label: "Retry freq", fmt: (v) => v.toFixed(2) },
  { key: "runsCompleted", label: "Completed" },
  { key: "runsFailed", label: "Failed" },
  { key: "rollbacks", label: "Rollbacks" },
  { key: "replans", label: "Re-plans" },
  { key: "policyBlocks", label: "Policy blocks" },
  { key: "avgRunLatencyMs", label: "Avg latency", fmt: (v) => `${v} ms` },
  { key: "mttrMs", label: "MTTR", fmt: (v) => `${v} ms` },
];

export function MetricsPanel() {
  const { connection } = useConnection();
  const { data } = usePolling<Metrics>(() => api.metrics(connection), 4000, true);

  return (
    <div className="panel">
      <h2>Reliability metrics</h2>
      {!data ? (
        <p className="muted">No data</p>
      ) : (
        <div className="metrics-grid">
          {FIELDS.map((f) => (
            <div className="metric" key={f.key}>
              <div className="val">
                {f.fmt ? f.fmt(data[f.key]) : data[f.key]}
              </div>
              <div className="lbl">{f.label}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
