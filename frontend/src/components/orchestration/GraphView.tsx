import type { WorkflowNode } from "../../api/types";
import { StatusBadge } from "../StatusBadge";

interface Props {
  nodes: WorkflowNode[];
  selectedKey: string | null;
  onSelect: (key: string) => void;
}

/** Groups nodes into dependency levels so parallel branches render on the same row. */
function levelsOf(nodes: WorkflowNode[]): WorkflowNode[][] {
  const byKey = new Map(nodes.map((n) => [n.nodeKey, n]));
  const levelCache = new Map<string, number>();

  function level(key: string, seen: Set<string>): number {
    if (levelCache.has(key)) return levelCache.get(key)!;
    if (seen.has(key)) return 0;
    seen.add(key);
    const node = byKey.get(key);
    if (!node || node.dependsOn.length === 0) {
      levelCache.set(key, 0);
      return 0;
    }
    const lvl = 1 + Math.max(...node.dependsOn.map((d) => level(d, seen)));
    levelCache.set(key, lvl);
    return lvl;
  }

  const rows: WorkflowNode[][] = [];
  for (const n of nodes) {
    const lvl = level(n.nodeKey, new Set());
    (rows[lvl] ||= []).push(n);
  }
  return rows.filter(Boolean);
}

export function GraphView({ nodes, selectedKey, onSelect }: Props) {
  const rows = levelsOf(nodes);

  return (
    <div className="graph">
      {rows.map((row, i) => (
        <div className="graph-row" key={i}>
          {row.map((node) => (
            <div
              key={node.nodeKey}
              className={`node-card ${selectedKey === node.nodeKey ? "selected" : ""}`}
              onClick={() => onSelect(node.nodeKey)}
            >
              <div className="nk">
                <span>
                  {node.nodeKey}
                  {node.requiresApproval && <span className="gate-tag">gate</span>}
                </span>
                <StatusBadge status={node.status} />
              </div>
              <div className="meta">
                attempts {node.attemptCount}/{node.maxAttempts}
                {node.dependsOn.length > 0 && <> · after {node.dependsOn.join(", ")}</>}
              </div>
              {node.lastError && <div className="meta" style={{ color: "#fca5a5" }}>{node.lastError}</div>}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
