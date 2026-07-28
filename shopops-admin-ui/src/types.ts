export type NaturalLanguageResult = {
  intent: string;
  intentLabel?: string;
  confidence: number;
  taskType: string;
  routedReason?: string;
  focusAreas?: string[];
  dataSources?: string[];
  recommendedActions?: string[];
  taskSpec?: AgentTaskSpec;
  plan?: AgentPlan;
  task: AgentTask;
};

export type AgentNaturalLanguageBatchEvaluationResponse = {
  available?: boolean;
  summaryPath?: string;
  checkedAt?: string;
  message?: string;
  summary?: AgentNaturalLanguageBatchEvaluationSummary;
};

export type AgentNaturalLanguageBatchEvaluationSummary = {
  generatedAt?: string;
  evaluationName?: string;
  dateRange?: {
    start?: string;
    end?: string;
    days?: number;
  };
  rounds?: number;
  caseCount?: number;
  passedCaseCount?: number;
  passRate?: number;
  successRate?: number;
  intentAccuracy?: number;
  toolInvocationCount?: number;
  toolSuccessCount?: number;
  toolInvocationSuccessRate?: number;
  avgToolInvocationCount?: number;
  avgWallClockDurationMs?: number;
  p95WallClockDurationMs?: number;
  avgTaskDurationMs?: number;
  statusBreakdown?: Record<string, number>;
  scenarioBreakdown?: Array<{
    scenario?: string;
    caseCount?: number;
    passedCaseCount?: number;
    successRate?: number;
    avgToolInvocationCount?: number;
    avgWallClockDurationMs?: number;
  }>;
};

export type AgentTaskSpec = {
  intent?: string;
  objective?: string;
  dateRange?: { start?: string; end?: string };
  focusAreas?: string[];
  requiredEvidence?: string[];
  outputFormat?: string;
  constraints?: Record<string, unknown>;
};

export type AgentPlan = {
  taskType?: string;
  rationale?: string;
  steps?: Array<{
    stepNo?: number;
    stepName?: string;
    toolCode?: string;
    reason?: string;
  }>;
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
  created?: number;
  queued?: number;
  running?: number;
  success?: number;
  failed?: number;
  degraded?: number;
  successRate?: number;
  avgLatencyMs?: number;
  statusBreakdown?: Record<string, number>;
};

export type AgentTaskDetail = {
  task?: AgentTask;
  steps?: AgentStep[];
  events?: AgentTaskEvent[];
  spans?: TraceSpan[];
  shopConfigSnapshot?: Record<string, unknown>;
  [key: string]: unknown;
};

export type TraceSpan = {
  traceId?: string;
  spanId?: string;
  parentSpanId?: string;
  spanType?: string;
  spanName?: string;
  refType?: string;
  refId?: number | string;
  status?: string;
  inputSummary?: string;
  outputSummary?: string;
  latencyMs?: number;
  errorMessage?: string;
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

export type McpTool = {
  toolCode?: string;
  toolName?: string;
  category?: string;
  permissionCode?: string;
  riskLevel?: string;
  needApproval?: boolean;
  enabled?: boolean;
  version?: string;
};

export type ToolCallLog = {
  id?: number | string;
  taskId?: number | string;
  stepId?: number | string;
  traceId?: string;
  spanId?: string;
  toolCode?: string;
  status?: string;
  riskLevel?: string;
  approvalId?: number | string;
  input?: Record<string, unknown>;
  output?: Record<string, unknown>;
  latencyMs?: number;
  retryCount?: number;
  errorCode?: string;
  errorMessage?: string;
  createdAt?: string;
};

export type ToolInvokeResult = {
  success?: boolean;
  status?: string;
  data?: unknown;
  toolCallLogId?: number | string;
  approvalId?: number | string;
  errorCode?: string;
  errorMessage?: string;
};

export type DashboardSummary = {
  taskMetrics?: AgentTaskMetrics;
  reportTotal?: number;
  toolCallTotal?: number;
  toolCallFailed?: number;
  recentFailedEvents?: AgentTaskEvent[];
  generatedAt?: string;
};

export type SystemHealth = {
  status?: string;
  persistence?: string;
  timestamp?: string;
  checks?: Record<string, HealthCheck>;
};

export type HealthCheck = {
  status?: string;
  mode?: string;
  message?: string;
  [key: string]: unknown;
};

export type PromptTemplate = {
  promptId?: number | string;
  tenantId?: number | string;
  promptCode?: string;
  promptName?: string;
  taskType?: string;
  templateContent?: string;
  version?: string;
  status?: string;
  createdBy?: number | string;
  createdAt?: string;
  updatedAt?: string;
};

export type PromptRenderResult = {
  promptCode?: string;
  version?: string;
  renderedPrompt?: string;
  variables?: Record<string, unknown>;
};

export type ModelCallLog = {
  callId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  userId?: number | string;
  username?: string;
  providerCode?: string;
  modelName?: string;
  promptCode?: string;
  promptVersion?: string;
  traceId?: string;
  taskId?: number | string;
  reportId?: number | string;
  status?: string;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  latencyMs?: number;
  errorCode?: string;
  errorMessage?: string;
  promptPreview?: string;
  outputPreview?: string;
  metadata?: Record<string, unknown>;
  createdAt?: string;
};

export type ApprovalRequest = {
  approvalId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  approvalNo?: string;
  sourceType?: string;
  sourceId?: number | string;
  taskId?: number | string;
  stepId?: number | string;
  traceId?: string;
  toolCode?: string;
  riskLevel?: string;
  title?: string;
  reason?: string;
  inputSummary?: string;
  status?: string;
  requesterId?: number | string;
  requesterName?: string;
  approverId?: number | string;
  approverName?: string;
  decisionComment?: string;
  createdAt?: string;
  decidedAt?: string;
};

export type ApprovalBatchDecisionResult = {
  requestedCount?: number;
  successCount?: number;
  failedCount?: number;
  succeeded?: ApprovalRequest[];
  failedApprovalIds?: Array<number | string>;
};

export type ConnectorStatus = {
  connectorCode?: string;
  connectorName?: string;
  category?: string;
  propertyKey?: string;
  configured?: boolean;
  available?: boolean;
  status?: string;
  configuredPath?: string;
  message?: string;
  lastCheckedAt?: string;
};

export type ConnectorCredential = {
  connectorCode?: string;
  credentialType?: string;
  maskedSecret?: string;
  configured?: boolean;
  enabled?: boolean;
  status?: string;
  expiresAt?: string;
  rotationStatus?: string;
  rotationMessage?: string;
  daysUntilExpiry?: number;
  updatedBy?: number | string;
  updatedAt?: string;
};

export type ConnectorCredentialTestResult = {
  connectorCode?: string;
  success?: boolean;
  status?: string;
  message?: string;
  testedAt?: string;
};

export type ConnectorSyncJob = {
  jobId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  connectorCode?: string;
  status?: string;
  attempt?: number;
  maxAttempts?: number;
  triggerType?: string;
  createdBy?: number | string;
  requestId?: string;
  message?: string;
  detail?: Record<string, unknown> | string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type ConnectorApiCallLog = {
  logId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  jobId?: number | string;
  connectorCode?: string;
  requestMethod?: string;
  endpoint?: string;
  requestTarget?: string;
  status?: string;
  statusCode?: number;
  latencyMs?: number;
  errorCode?: string;
  errorMessage?: string;
  requestId?: string;
  detail?: Record<string, unknown> | string;
  createdAt?: string;
};

export type OrganizationOverview = {
  tenantTotal?: number;
  shopTotal?: number;
  userTotal?: number;
  activeMemberTotal?: number;
  disabledMemberTotal?: number;
};

export type OrganizationUser = {
  userId?: number | string;
  username?: string;
  displayName?: string;
  email?: string;
  phone?: string;
  status?: string;
  tenantRoles?: string[];
  shopRoles?: string[];
  createdAt?: string;
};

export type Tenant = {
  tenantId?: number | string;
  tenantNo?: string;
  tenantName?: string;
  status?: string;
  planType?: string;
  contactName?: string;
  contactPhone?: string;
  shopCount?: number;
  memberCount?: number;
  createdAt?: string;
};

export type Shop = {
  shopId?: number | string;
  tenantId?: number | string;
  shopNo?: string;
  shopName?: string;
  platformType?: string;
  ownerId?: number | string;
  status?: string;
  memberCount?: number;
  createdAt?: string;
};

export type ShopMember = {
  memberId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  shopName?: string;
  userId?: number | string;
  username?: string;
  displayName?: string;
  roleCode?: string;
  normalizedRole?: string;
  status?: string;
  joinedAt?: string;
};

export type ShopConfig = {
  configId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  configKey?: string;
  configValue?: string;
  valueType?: string;
  updatedBy?: number | string;
  updatedAt?: string;
};

export type CurrentUser = {
  tenantId?: number | string;
  shopId?: number | string;
  userId?: number | string;
  username?: string;
  roles?: string[];
  authType?: string;
  authenticated?: boolean;
  requestId?: string;
};

export type LoginResult = {
  tokenType?: string;
  accessToken?: string;
  expiresAt?: string;
  user?: CurrentUser;
};

export type LogoutResult = {
  tokenId?: string;
  status?: string;
};

export type AuthAuditEvent = {
  eventId?: number | string;
  tenantId?: number | string;
  shopId?: number | string;
  userId?: number | string;
  username?: string;
  eventType?: string;
  eventStatus?: string;
  authType?: string;
  requestId?: string;
  clientIp?: string;
  userAgent?: string;
  failureReason?: string;
  createdAt?: string;
};
