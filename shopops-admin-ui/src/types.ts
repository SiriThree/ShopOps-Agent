export type NaturalLanguageResult = {
  intent: string;
  intentLabel?: string;
  confidence: number;
  taskType: string;
  routedReason?: string;
  focusAreas?: string[];
  dataSources?: string[];
  recommendedActions?: string[];
  task: AgentTask;
};

export type AgentTask = {
  taskId: number | string;
  taskNo?: string;
  taskType?: string;
  status?: string;
  traceId?: string;
  reportId?: number | string;
  userId?: number | string;
  createdAt?: string;
  errorMessage?: string;
  userInput?: string;
  resultSummary?: string;
};

export type AgentStep = {
  stepNo?: number | string;
  stepName?: string;
  toolCode?: string;
  status?: string;
  output?: unknown;
};

export type AgentTaskEvent = {
  eventId?: number | string;
  taskId?: number | string;
  taskNo?: string;
  eventType?: string;
  fromStatus?: string;
  toStatus?: string;
  eventStatus?: string;
  message?: string;
  createdAt?: string;
};

export type AgentTaskMetrics = {
  total?: number;
  success?: number;
  failed?: number;
  statusBreakdown?: Record<string, number>;
};

export type AgentTaskDetail = {
  task?: AgentTask;
  steps?: AgentStep[];
  events?: AgentTaskEvent[];
  shopConfigSnapshot?: Record<string, unknown>;
  [key: string]: unknown;
};

export type AgentTaskCreateResult = {
  taskId: number | string;
  taskNo?: string;
};

export type AgentTaskRecoveryResult = {
  requeuedCount?: number;
  taskIds?: Array<number | string>;
};

export type PageResult<T> = {
  list?: T[];
  total?: number;
};

export type OperationReport = {
  reportId?: number | string;
  reportNo?: string;
  reportType?: string;
  status?: string;
  taskId?: number | string;
  traceId?: string;
  createdAt?: string;
  createdBy?: number | string;
  title?: string;
  summary?: string;
  markdown?: string;
  evidence?: Record<string, unknown> | string;
};

export type Evidence = {
  intent?: string;
  riskCommentIds?: unknown[];
  productIds?: unknown[];
  campaignNames?: unknown[];
  channelNames?: unknown[];
  toolCodes?: string[];
  generationMode?: string;
  modelProviderCode?: string;
  modelCallId?: number | string;
  shopConfig?: Record<string, unknown>;
  dataSources?: DataSourceEvidence;
};

export type DataSourceEvidence = {
  orderSummary?: SourceMetrics;
  negativeComments?: SourceMetrics;
  productCandidates?: SourceMetrics;
  adPerformance?: SourceMetrics;
  externalReports?: SourceMetrics;
};

export type SourceMetrics = {
  connectorCode?: string;
  metrics?: Record<string, unknown>;
};

export type AuditOverview = {
  authEventTotal?: number;
  authFailureTotal?: number;
  taskEventTotal?: number;
  taskFailureTotal?: number;
  toolCallTotal?: number;
  toolCallFailed?: number;
  recentAuthEvents?: Record<string, unknown>[];
  recentTaskEvents?: AgentTaskEvent[];
  recentToolCalls?: Record<string, unknown>[];
  generatedAt?: string;
};

export type AuditRiskSummary = {
  total?: number;
  elevatedRiskTotal?: number;
  riskBreakdown?: Record<string, number>;
  recentElevatedRiskEvents?: AuditTimelineEvent[];
  generatedAt?: string;
};

export type AuditTimelineEvent = {
  source?: string;
  eventId?: string;
  eventType?: string;
  eventStatus?: string;
  userId?: number | string;
  username?: string;
  taskId?: number | string;
  traceId?: string;
  toolCode?: string;
  requestId?: string;
  resourceType?: string;
  resourceId?: string;
  riskLevel?: string;
  summary?: string;
  detail?: Record<string, unknown>;
  createdAt?: string;
};

export type AuditTimelineDetail = {
  event?: AuditTimelineEvent;
  resource?: Record<string, unknown>;
  context?: Record<string, unknown>;
};
