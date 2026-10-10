import type { AuditEvent } from "../../api/types";

export function AuditTrail({ events }: { events: AuditEvent[] }) {
  return (
    <div className="panel">
      <h2>Audit trail</h2>
      {events.length === 0 ? (
        <p className="muted">No events yet</p>
      ) : (
        <div className="audit-list">
          {events.map((e) => (
            <div className="audit-item" key={e.id}>
              <div>
                <span className="evt">{e.eventType}</span>
                {e.nodeKey && <span className="muted"> · {e.nodeKey}</span>}
              </div>
              {e.detail && <div className="muted">{e.detail}</div>}
              <div className="muted" style={{ fontSize: 10 }}>
                {new Date(e.createdAt).toLocaleTimeString()}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
