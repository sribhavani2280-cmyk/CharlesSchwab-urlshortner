import type {
  Analytics,
  Approval,
  AuditEvent,
  CreateLinkRequest,
  Metrics,
  ShortLink,
  WorkflowRun,
} from "./types";

export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export interface Connection {
  baseUrl: string;
  apiKey: string;
}

async function request<T>(
  conn: Connection,
  method: string,
  path: string,
  body?: unknown,
): Promise<T> {
  const res = await fetch(`${conn.baseUrl}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      "X-Api-Key": conn.apiKey,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`;
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {
      // non-JSON error body; keep status text
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  // URL links
  createLink: (c: Connection, req: CreateLinkRequest) =>
    request<ShortLink>(c, "POST", "/api/v1/links", req),
  listLinks: (c: Connection) => request<ShortLink[]>(c, "GET", "/api/v1/links"),
  disableLink: (c: Connection, code: string) =>
    request<ShortLink>(c, "POST", `/api/v1/links/${code}/disable`),
  enableLink: (c: Connection, code: string) =>
    request<ShortLink>(c, "POST", `/api/v1/links/${code}/enable`),
  deleteLink: (c: Connection, code: string) =>
    request<void>(c, "DELETE", `/api/v1/links/${code}`),
  analytics: (c: Connection, code: string) =>
    request<Analytics>(c, "GET", `/api/v1/links/${code}/analytics`),

  // Orchestration
  listRuns: (c: Connection) =>
    request<WorkflowRun[]>(c, "GET", "/api/v1/orchestration/runs"),
  getRun: (c: Connection, id: string) =>
    request<WorkflowRun>(c, "GET", `/api/v1/orchestration/runs/${id}`),
  startRun: (
    c: Connection,
    req: { name: string; requirement: string; scenarioType: string },
  ) => request<WorkflowRun>(c, "POST", "/api/v1/orchestration/runs", req),
  approve: (
    c: Connection,
    id: string,
    nodeKey: string,
    approved: boolean,
    reason: string,
    decidedBy: string,
  ) =>
    request<WorkflowRun>(
      c,
      "POST",
      `/api/v1/orchestration/runs/${id}/nodes/${nodeKey}/approve`,
      { approved, reason, decidedBy },
    ),
  replan: (c: Connection, id: string, requirement: string) =>
    request<WorkflowRun>(c, "POST", `/api/v1/orchestration/runs/${id}/replan`, {
      requirement,
    }),
  rollback: (c: Connection, id: string, nodeKey: string) =>
    request<WorkflowRun>(
      c,
      "POST",
      `/api/v1/orchestration/runs/${id}/nodes/${nodeKey}/rollback`,
    ),
  resume: (c: Connection, id: string) =>
    request<WorkflowRun>(c, "POST", `/api/v1/orchestration/runs/${id}/resume`),
  cancel: (c: Connection, id: string) =>
    request<WorkflowRun>(c, "POST", `/api/v1/orchestration/runs/${id}/cancel`),
  audit: (c: Connection, id: string) =>
    request<AuditEvent[]>(c, "GET", `/api/v1/orchestration/runs/${id}/audit`),
  approvals: (c: Connection, id: string) =>
    request<Approval[]>(c, "GET", `/api/v1/orchestration/runs/${id}/approvals`),
  metrics: (c: Connection) =>
    request<Metrics>(c, "GET", "/api/v1/orchestration/metrics"),
};
