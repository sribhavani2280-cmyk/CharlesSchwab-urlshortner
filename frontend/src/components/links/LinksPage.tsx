import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../../api/client";
import { useConnection } from "../../hooks/useConnection";
import type { ShortLink } from "../../api/types";
import { CreateLinkForm } from "./CreateLinkForm";
import { LinksTable } from "./LinksTable";

export function LinksPage() {
  const { connection } = useConnection();
  const [links, setLinks] = useState<ShortLink[]>([]);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      setLinks(await api.listLinks(connection));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not reach backend");
    }
  }, [connection]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <div className="grid-2">
      <div>
        <CreateLinkForm
          onCreated={(l) => setLinks((prev) => [l, ...prev.filter((p) => p.code !== l.code)])}
        />
      </div>
      <div className="panel">
        <div className="inline" style={{ justifyContent: "space-between" }}>
          <h2>Links</h2>
          <button onClick={refresh}>Refresh</button>
        </div>
        {error && <div className="error">{error}</div>}
        <LinksTable links={links} onChanged={refresh} />
      </div>
    </div>
  );
}
