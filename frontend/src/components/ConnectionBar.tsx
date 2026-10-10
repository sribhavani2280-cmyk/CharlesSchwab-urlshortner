import { useState } from "react";
import { useConnection } from "../hooks/useConnection";

export function ConnectionBar() {
  const { connection, setConnection } = useConnection();
  const [baseUrl, setBaseUrl] = useState(connection.baseUrl);
  const [apiKey, setApiKey] = useState(connection.apiKey);

  const dirty = baseUrl !== connection.baseUrl || apiKey !== connection.apiKey;

  return (
    <div className="panel">
      <div className="row" style={{ alignItems: "flex-end" }}>
        <div className="field" style={{ flex: 2, marginBottom: 0 }}>
          <label htmlFor="baseUrl">Backend base URL</label>
          <input
            id="baseUrl"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
          />
        </div>
        <div className="field" style={{ flex: 1, marginBottom: 0 }}>
          <label htmlFor="apiKey">API key (X-Api-Key)</label>
          <input
            id="apiKey"
            type="password"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
          />
        </div>
        <button
          className="primary"
          disabled={!dirty}
          onClick={() => setConnection({ baseUrl: baseUrl.trim(), apiKey: apiKey.trim() })}
        >
          Save
        </button>
      </div>
    </div>
  );
}
