import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { useConnection } from "../../hooks/useConnection";
import type { ShortLink } from "../../api/types";

export function CreateLinkForm({ onCreated }: { onCreated: (link: ShortLink) => void }) {
  const { connection } = useConnection();
  const [url, setUrl] = useState("");
  const [alias, setAlias] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const link = await api.createLink(connection, {
        url: url.trim(),
        alias: alias.trim() || undefined,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      });
      onCreated(link);
      setUrl("");
      setAlias("");
      setExpiresAt("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Create short link</h2>
      {error && <div className="error">{error}</div>}
      <div className="field">
        <label htmlFor="url">Destination URL</label>
        <input
          id="url"
          placeholder="https://example.com/page"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          required
        />
      </div>
      <div className="row">
        <div className="field" style={{ flex: 1 }}>
          <label htmlFor="alias">Custom alias (optional)</label>
          <input
            id="alias"
            placeholder="launch-2026"
            value={alias}
            onChange={(e) => setAlias(e.target.value)}
          />
        </div>
        <div className="field" style={{ flex: 1 }}>
          <label htmlFor="exp">Expires at (optional)</label>
          <input
            id="exp"
            type="datetime-local"
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
          />
        </div>
      </div>
      <button className="primary" type="submit" disabled={busy || !url.trim()}>
        {busy ? "Creating..." : "Create"}
      </button>
    </form>
  );
}
