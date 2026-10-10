export interface ShortLink {
  code: string;
  shortUrl: string;
  longUrl: string;
  active: boolean;
  customAlias: boolean;
  clickCount: number;
  createdAt: string;
  expiresAt: string | null;
}

export interface CreateLinkRequest {
  url: string;
  alias?: string;
  expiresAt?: string;
}

export interface Analytics {
  code: string;
  totalClicks: number;
  windowStart: string;
  clicksByDay: Record<string, number>;
  topReferers: string[];
}

export type NodeStatus =
  | "PENDING"
  | "AWAITING_APPROVAL"
  | "RUNNING"
  | "SUCCEEDED"
  | "FAILED"
  | "ROLLED_BACK"
  | "CANCELLED";

export type RunStatus =
  | "CREATED"
  | "RUNNING"
  | "AWAITING_APPROVAL"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export interface WorkflowNode {
  nodeKey: string;
  role: string;
  status: NodeStatus;
  dependsOn: string[];
  requiresApproval: boolean;
  attemptCount: number;
  maxAttempts: number;
  outputHash: string | null;
  output: string | null;
  lastError: string | null;
  updatedAt: string;
}

export interface WorkflowRun {
  id: string;
  name: string;
  scenarioType: string;
  status: RunStatus;
  requirementText: string;
  correlationId: string;
  createdAt: string;
  updatedAt: string;
  nodes: WorkflowNode[];
}

export interface AuditEvent {
  id: number;
  runId: string;
  correlationId: string;
  nodeKey: string | null;
  eventType: string;
  detail: string | null;
  createdAt: string;
}

export interface Approval {
  id: number;
  runId: string;
  nodeKey: string;
  decision: string;
  artifactHash: string | null;
  reason: string | null;
  decidedBy: string | null;
  decidedAt: string;
}

export interface Metrics {
  runsStarted: number;
  runsCompleted: number;
  runsFailed: number;
  nodeAttempts: number;
  nodeRetries: number;
  nodeFailures: number;
  rollbacks: number;
  replans: number;
  policyBlocks: number;
  approvalsApproved: number;
  approvalsRejected: number;
  nodeSuccessRate: number;
  retryFrequency: number;
  runSuccessRate: number;
  avgRunLatencyMs: number;
  mttrMs: number;
}
