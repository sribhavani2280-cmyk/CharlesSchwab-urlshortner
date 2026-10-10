import { useState } from "react";
import { api, ApiError } from "../../api/client";
import { useConnection } from "../../hooks/useConnection";
import type { Analytics, ShortLink } from "../../api/types";

interface Props {
  links: ShortLink[];
  onChanged: () => void;
}

export function LinksTable({ links, onChanged }: Props) {
  const { connection } = useConnection();
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function act(fn: () => Promise<unknown>) {
    setError(null);
    try {
      await fn();
      onChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Request failed");
    }
  }

  if (links.length === 0) {
    return <p className="muted">No links yet. Create one above.</p>;
  }

  return (
    <div>
      {error && <div className="error">{error}</div>}
      <table>
        <thead>
          <tr>
            <th>Code</th>
            <th>Destination</th>
            <th>Status</th>
            <th>Clicks</th>
            <th>Expires</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {links.map((l) => (
            <tr key={l.code}>
              <td>
                <a href={l.shortUrl} target="_blank" rel="noreferrer">
                  {l.code}
                </a>
                {l.customAlias && <span className="gate-tag">alias</span>}
              </td>
              <td style={{ maxWidth: 280, overflow: "hidden", textOverflow: "ellipsis" }}>
                {l.longUrl}
              </td>
              <td>
                <span className={`badge ${l.active ? "SUCCEEDED" : "FAILED"}`}>
                  {l.active ? "active" : "disabled"}
                </span>
              </td>
              <td>{l.clickCount}</td>
              <td className="muted">
                {l.expiresAt ? new Date(l.expiresAt).toLocaleString() : "—"}
              </td>
              <td>
                <div className="inline">
                  {l.active ? (
                    <button onClick={() => act(() => api.disableLink(connection, l.code))}>
                      Disable
                    </button>
                  ) : (
                    <button onClick={() => act(() => api.enableLink(connection, l.code))}>
                      Enable
                    </button>
                  )}
                  <button
                    onClick={async () => {
                      setError(null);
                      try {
                        setAnalytics(await api.analytics(connection, l.code));
                      } catch (err) {
                        setError(err instanceof ApiError ? err.message : "Request failed");
                      }
                    }}
                  >
                    Analytics
                  </button>
                  <button
                    className="bad"
                    onClick={() => act(() => api.deleteLink(connection, l.code))}
                  >
                    Delete
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {analytics && (
        <div className="panel" style={{ marginTop: 16 }}>
          <div className="inline" style={{ justifyContent: "space-between" }}>
            <h2>Analytics — {analytics.code}</h2>
            <button onClick={() => setAnalytics(null)}>Close</button>
          </div>
          <p>
            Total clicks: <strong>{analytics.totalClicks}</strong>
          </p>
          <p className="muted">
            Window since {new Date(analytics.windowStart).toLocaleDateString()}
          </p>
          <table>
            <thead>
              <tr>
                <th>Day</th>
                <th>Clicks</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(analytics.clicksByDay).length === 0 ? (
                <tr>
                  <td colSpan={2} className="muted">
                    No clicks in window
                  </td>
                </tr>
              ) : (
                Object.entries(analytics.clicksByDay).map(([day, count]) => (
                  <tr key={day}>
                    <td>{day}</td>
                    <td>{count}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {analytics.topReferers.length > 0 && (
            <p className="muted">Top referers: {analytics.topReferers.join(", ")}</p>
          )}
        </div>
      )}
    </div>
  );
}
