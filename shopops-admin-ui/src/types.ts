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
