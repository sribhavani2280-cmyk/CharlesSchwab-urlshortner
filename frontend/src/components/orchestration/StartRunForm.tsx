import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { useConnection } from "../../hooks/useConnection";
import type { WorkflowRun } from "../../api/types";

const PRESETS: Record<string, { name: string; requirement: string }> = {
  greenfield: {
    name: "Greenfield: custom aliases",
    requirement: "Add custom alias support to the URL shortener so users can choose a memorable code.",
  },
  brownfield: {
    name: "Brownfield: fix expired redirect",
    requirement:
      "Fix a defect where expired links still redirect instead of returning an unavailable response.",
  },
  ambiguous: {
    name: "Ambiguous: branded links",
    requirement: "Support branded links and better analytics.",
  },
};

export function StartRunForm({ onStarted }: { onStarted: (run: WorkflowRun) => void }) {
  const { connection } = useConnection();
  const [scenarioType, setScenarioType] = useState("greenfield");
  const [name, setName] = useState(PRESETS.greenfield.name);
  const [requirement, setRequirement] = useState(PRESETS.greenfield.requirement);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function applyPreset(scenario: string) {
    setScenarioType(scenario);
    const preset = PRESETS[scenario];
    if (preset) {
      setName(preset.name);
      setRequirement(preset.requirement);
    }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const run = await api.startRun(connection, {
        name: name.trim(),
        requirement: requirement.trim(),
        scenarioType,
      });
      onStarted(run);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Start a run</h2>
      {error && <div className="error">{error}</div>}
      <div className="field">
        <label htmlFor="scenario">Scenario</label>
        <select
          id="scenario"
          value={scenarioType}
          onChange={(e) => applyPreset(e.target.value)}
        >
          <option value="greenfield">Greenfield</option>
          <option value="brownfield">Brownfield</option>
          <option value="ambiguous">Ambiguous</option>
        </select>
      </div>
      <div className="field">
        <label htmlFor="name">Run name</label>
        <input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
      </div>
      <div className="field">
        <label htmlFor="req">Requirement</label>
        <textarea
          id="req"
          rows={4}
          value={requirement}
          onChange={(e) => setRequirement(e.target.value)}
          required
        />
      </div>
      <button className="primary" type="submit" disabled={busy}>
        {busy ? "Starting..." : "Start run"}
      </button>
    </form>
  );
}
