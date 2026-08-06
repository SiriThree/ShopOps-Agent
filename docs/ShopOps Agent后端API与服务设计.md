# ShopOps Agent 后端 API 与 Controller/Service 设计

## 1. 设计目标

本文档将 ShopOps Agent 企业级平台方案和 ER 设计进一步落到 Java 后端接口与服务层设计。整体风格参考 mall-master 的 Spring Boot 分层方式：

```text
Controller -> Service -> Mapper/Dao -> MySQL
```

同时针对 Agent 平台新增以下企业级边界：

```text
Agent Engine -> Tool Gateway -> Tool Executor -> Connector / Business Data
Workflow -> Approval -> Risk Control
Trace -> Tool Log / Model Log / Task Event
```

设计目标：

- API 清晰覆盖租户、店铺、工具、任务、审批、报告、模型和审计。
- Service 职责边界清晰，避免 Controller 堆业务逻辑。
- Agent 不直接访问数据库或外部平台，所有能力通过 Tool Gateway。
- 高风险工具调用必须通过审批服务。
- 所有任务、工具调用、模型调用和审批操作可追踪。

## 2. 通用 API 约定

### 2.1 统一返回

沿用 mall-master 风格：

```java
CommonResult<T>
CommonPage<T>
```

示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 2.2 通用请求头

```text
Authorization: Bearer <token>
X-Tenant-Id: <tenantId>
X-Shop-Id: <shopId>
X-Request-Id: <requestId>
```

说明：

- `X-Tenant-Id` 用于租户上下文。
- `X-Shop-Id` 用于当前店铺上下文。
- `X-Request-Id` 用于接口级追踪，后端可转换为 `trace_id`。

### 2.3 权限规则

API 权限由 Spring Security 动态资源控制。

工具权限由 Tool Gateway 二次校验。

```text
API Permission: /agent/tasks/create
Tool Permission: order:read
Data Scope: tenant_id + shop_id
Risk Policy: risk_level + need_approval
```

## 3. 后端包结构

```text
com.shopops
├── common
├── security
├── tenant
├── shop
├── connector
├── tool
├── agent
├── workflow
├── report
├── model
└── audit
```

每个业务包内部建议：

```text
controller
service
service.impl
dto
domain
mapper
dao
component
```

## 4. 租户与店铺 API

### 4.1 TenantController

职责：租户创建、租户列表、成员管理、租户上下文切换。

```text
POST   /api/tenants
GET    /api/tenants
GET    /api/tenants/{tenantId}
POST   /api/tenants/{tenantId}/members
GET    /api/tenants/{tenantId}/members
POST   /api/tenants/{tenantId}/members/{userId}/role
POST   /api/tenants/{tenantId}/members/{userId}/disable
```

核心 Service：

```java
public interface TenantService {
    Long createTenant(TenantCreateParam param);
    List<TenantDto> listCurrentUserTenants(Long userId);
    TenantDto getTenant(Long tenantId);
    int addMember(Long tenantId, TenantMemberParam param);
    List<TenantMemberDto> listMembers(Long tenantId);
    int updateMemberRole(Long tenantId, Long userId, String roleCode);
    int disableMember(Long tenantId, Long userId);
}
```

### 4.2 ShopController

职责：店铺创建、成员管理、配置管理、当前店铺上下文。

```text
POST   /api/shops
GET    /api/shops
GET    /api/shops/{shopId}
POST   /api/shops/{shopId}/members
GET    /api/shops/{shopId}/members
GET    /api/shops/{shopId}/configs
POST   /api/shops/{shopId}/configs
PUT    /api/shops/{shopId}/configs/{configKey}
```

核心 Service：

```java
public interface ShopService {
    Long createShop(Long tenantId, ShopCreateParam param);
    List<ShopDto> listCurrentUserShops(Long tenantId, Long userId);
    ShopDto getShop(Long tenantId, Long shopId);
    int addMember(Long tenantId, Long shopId, ShopMemberParam param);
    List<ShopMemberDto> listMembers(Long tenantId, Long shopId);
    Map<String, Object> getConfigMap(Long tenantId, Long shopId);
    int upsertConfig(Long tenantId, Long shopId, ShopConfigParam param);
}
```

## 5. 连接器 API

### 5.1 ConnectorController

职责：管理平台连接器目录。

```text
GET    /api/connectors
GET    /api/connectors/{connectorCode}
POST   /api/connectors
PUT    /api/connectors/{connectorCode}
POST   /api/connectors/{connectorCode}/enable
POST   /api/connectors/{connectorCode}/disable
```

核心 Service：

```java
public interface ConnectorService {
    List<ConnectorDto> listConnectors();
    ConnectorDto getConnector(String connectorCode);
    Long createConnector(ConnectorCreateParam param);
    int updateConnector(String connectorCode, ConnectorUpdateParam param);
    int updateEnabled(String connectorCode, Boolean enabled);
}
```

### 5.2 ConnectorAccountController

职责：店铺绑定外部平台账号或模拟数据源。

```text
POST   /api/shops/{shopId}/connector-accounts
GET    /api/shops/{shopId}/connector-accounts
GET    /api/shops/{shopId}/connector-accounts/{accountId}
PUT    /api/shops/{shopId}/connector-accounts/{accountId}
POST   /api/shops/{shopId}/connector-accounts/{accountId}/test
POST   /api/shops/{shopId}/connector-accounts/{accountId}/sync
GET    /api/shops/{shopId}/connector-sync-logs
```

核心 Service：

```java
public interface ConnectorAccountService {
    Long bindAccount(Long tenantId, Long shopId, ConnectorAccountParam param);
    List<ConnectorAccountDto> listAccounts(Long tenantId, Long shopId);
    ConnectorAccountDto getAccount(Long tenantId, Long shopId, Long accountId);
    int updateAccount(Long tenantId, Long shopId, Long accountId, ConnectorAccountParam param);
    ConnectorTestResult testConnection(Long tenantId, Long shopId, Long accountId);
    Long triggerSync(Long tenantId, Long shopId, Long accountId, String syncType);
    List<ConnectorSyncLogDto> listSyncLogs(Long tenantId, Long shopId, ConnectorSyncLogQuery query);
}
```

## 6. 工具中心 API

### 6.1 McpToolController

职责：管理工具注册、Schema、风险等级、权限码和启停状态。

```text
POST   /api/tools
GET    /api/tools
GET    /api/tools/{toolCode}
PUT    /api/tools/{toolCode}
POST   /api/tools/{toolCode}/enable
POST   /api/tools/{toolCode}/disable
POST   /api/tools/{toolCode}/versions
GET    /api/tools/{toolCode}/versions
POST   /api/tools/{toolCode}/validate
```

核心 Service：

```java
public interface McpToolService {
    Long createTool(Long tenantId, McpToolCreateParam param);
    CommonPage<McpToolDto> listTools(Long tenantId, McpToolQuery query, Integer pageSize, Integer pageNum);
    McpToolDto getTool(Long tenantId, String toolCode);
    int updateTool(Long tenantId, String toolCode, McpToolUpdateParam param);
    int updateEnabled(Long tenantId, String toolCode, Boolean enabled);
    Long createVersion(Long tenantId, String toolCode, ToolVersionParam param);
    List<ToolVersionDto> listVersions(Long tenantId, String toolCode);
    ToolSchemaValidateResult validateSchema(McpToolSchemaValidateParam param);
}
```

### 6.2 ToolInvokeController

职责：手动调试工具。生产环境应限制权限，只允许管理员或开发角色使用。

```text
POST   /api/tools/{toolCode}/invoke
GET    /api/tools/call-logs
GET    /api/tools/call-logs/{logId}
```

核心 Service：

```java
public interface ToolGatewayService {
    ToolInvokeResult invoke(ToolInvokeContext context, String toolCode, Object input);
    ToolInvokeResult invokeFromAgent(AgentToolInvokeCommand command);
    ToolPermissionCheckResult checkPermission(ToolInvokeContext context, McpToolDto tool);
    ToolRiskCheckResult checkRisk(ToolInvokeContext context, McpToolDto tool, Object input);
}
```

Tool Gateway 是企业级 ShopOps 的关键服务，不允许绕过。

调用链：

```text
Controller / Agent Executor
  -> ToolGatewayService
  -> McpToolService
  -> PermissionService
  -> JsonSchemaValidator
  -> RiskPolicyService
  -> ToolExecutorRegistry
  -> ToolExecutor
  -> ToolCallLogService
```

### 6.3 ToolExecutor 设计

```java
public interface ToolExecutor {
    String toolCode();
    ToolInvokeResult execute(ToolInvokeContext context, Object input);
}
```

示例实现：

```text
OrderQuerySummaryExecutor
CommentQueryNegativeExecutor
ProductGenerateTitleCandidatesExecutor
ProductUpdateTitleExecutor
AdQueryPerformanceExecutor
ReportExportExcelExecutor
FeishuSyncReportExecutor
```

## 7. Agent 任务 API

### 7.1 AgentTaskController

职责：创建任务、查询任务、取消任务、重试任务。

```text
POST   /api/agent/tasks
GET    /api/agent/tasks
GET    /api/agent/tasks/{taskId}
GET    /api/agent/tasks/{taskId}/steps
GET    /api/agent/tasks/{taskId}/events
POST   /api/agent/tasks/{taskId}/cancel
POST   /api/agent/tasks/{taskId}/retry
POST   /api/agent/tasks/{taskId}/run-sync
```

说明：

- `POST /api/agent/tasks` 默认创建异步任务。
- `run-sync` 只用于开发调试或轻量任务。

核心 Service：

```java
public interface AgentTaskService {
    Long createTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param);
    CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQuery query, Integer pageSize, Integer pageNum);
    AgentTaskDetailDto getTaskDetail(Long tenantId, Long shopId, Long taskId);
    List<AgentTaskStepDto> listSteps(Long tenantId, Long shopId, Long taskId);
    List<AgentTaskEventDto> listEvents(Long tenantId, Long shopId, Long taskId);
    int cancelTask(Long tenantId, Long shopId, Long taskId, Long operatorId);
    int retryTask(Long tenantId, Long shopId, Long taskId, Long operatorId);
    AgentTaskResult runSync(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param);
}
```

### 7.2 AgentEngineService

职责：任务编排主入口。

```java
public interface AgentEngineService {
    void executeTask(Long taskId);
    AgentPlan plan(AgentTaskContext context);
    AgentExecutionResult executePlan(AgentTaskContext context, AgentPlan plan);
    VerificationResult verify(AgentTaskContext context, AgentExecutionResult result);
}
```

内部服务：

```java
public interface PlannerService {
    AgentPlan createPlan(AgentTaskContext context);
}

public interface PlanValidator {
    PlanValidateResult validate(AgentTaskContext context, AgentPlan plan);
}

public interface ExecutorService {
    AgentExecutionResult execute(AgentTaskContext context, AgentPlan plan);
}

public interface VerifierService {
    VerificationResult verify(AgentTaskContext context, AgentExecutionResult result);
}
```

### 7.3 Agent Worker

异步任务消费者：

```text
AgentTaskSender
AgentTaskReceiver
AgentTaskRetryReceiver
AgentTaskDeadLetterReceiver
```

队列：

```text
shopops.agent.task.execute
shopops.agent.task.retry
shopops.agent.task.dead
```

Worker 逻辑：

```text
1. 获取 taskId
2. 查询 agent_task
3. 检查任务状态是否可执行
4. 状态改为 RUNNING
5. 调用 AgentEngineService.executeTask
6. 根据结果更新 SUCCESS / PARTIAL_SUCCESS / DEGRADED / FAILED
7. 写入 agent_task_event
```

## 8. 审批 API

### 8.1 ApprovalController

职责：待审批列表、审批详情、通过、驳回、修改后通过、执行结果查询。

```text
GET    /api/approvals/pending
GET    /api/approvals
GET    /api/approvals/{approvalId}
POST   /api/approvals/{approvalId}/approve
POST   /api/approvals/{approvalId}/reject
POST   /api/approvals/{approvalId}/modify-approve
POST   /api/approvals/{approvalId}/cancel
GET    /api/approvals/{approvalId}/actions
```

核心 Service：

```java
public interface ApprovalService {
    Long createApproval(ApprovalCreateCommand command);
    CommonPage<ApprovalDto> listApprovals(Long tenantId, Long shopId, ApprovalQuery query, Integer pageSize, Integer pageNum);
    ApprovalDetailDto getApproval(Long tenantId, Long shopId, Long approvalId);
    int approve(Long tenantId, Long shopId, Long approvalId, Long approverId, ApprovalActionParam param);
    int reject(Long tenantId, Long shopId, Long approvalId, Long approverId, ApprovalActionParam param);
    int modifyAndApprove(Long tenantId, Long shopId, Long approvalId, Long approverId, ApprovalModifyParam param);
    int cancel(Long tenantId, Long shopId, Long approvalId, Long operatorId);
    List<ApprovalActionLogDto> listActions(Long tenantId, Long approvalId);
}
```

审批通过后的执行：

```text
ApprovalService
  -> RiskPolicyService
  -> ToolGatewayService.invoke approved command
  -> update approval.execution_status
  -> update agent_task_step.status
  -> append agent_task_event
```

## 9. 报告 API

### 9.1 OperationReportController

职责：报告列表、报告详情、导出、重新生成。

```text
GET    /api/reports
GET    /api/reports/{reportId}
GET    /api/reports/{reportId}/evidence
POST   /api/reports/{reportId}/export
POST   /api/reports/{reportId}/regenerate
GET    /api/reports/export-logs
```

核心 Service：

```java
public interface OperationReportService {
    Long createReport(ReportCreateCommand command);
    CommonPage<OperationReportDto> listReports(Long tenantId, Long shopId, ReportQuery query, Integer pageSize, Integer pageNum);
    OperationReportDetailDto getReport(Long tenantId, Long shopId, Long reportId);
    ReportEvidenceDto getEvidence(Long tenantId, Long shopId, Long reportId);
    ReportExportResult exportReport(Long tenantId, Long shopId, Long reportId, Long operatorId, ReportExportParam param);
    Long regenerate(Long tenantId, Long shopId, Long reportId, Long operatorId);
}
```

报告生成服务：

```java
public interface ReportGenerationService {
    ReportDraft generateDailyReview(AgentTaskContext context, AgentExecutionResult executionResult);
    ReportDraft generateNegativeCommentReport(AgentTaskContext context, AgentExecutionResult executionResult);
    ReportDraft generateTitleOptimizationReport(AgentTaskContext context, AgentExecutionResult executionResult);
}
```

## 10. 模型网关 API

### 10.1 ModelProviderController

职责：模型供应商、模型路由策略、启停管理。

```text
POST   /api/model-providers
GET    /api/model-providers
GET    /api/model-providers/{providerCode}
PUT    /api/model-providers/{providerCode}
POST   /api/model-providers/{providerCode}/enable
POST   /api/model-providers/{providerCode}/disable
POST   /api/model-providers/{providerCode}/test
GET    /api/model-call-logs
```

核心 Service：

```java
public interface ModelGatewayService {
    ChatResponse chat(ModelRequestContext context, ChatRequest request);
    JsonResponse generateJson(ModelRequestContext context, JsonRequest request);
    ModelProviderDto route(ModelRequestContext context);
}
```

### 10.2 PromptTemplateController

```text
POST   /api/prompts
GET    /api/prompts
GET    /api/prompts/{promptCode}
POST   /api/prompts/{promptCode}/versions
POST   /api/prompts/{promptCode}/enable
POST   /api/prompts/{promptCode}/rollback
POST   /api/prompts/{promptCode}/render-test
```

核心 Service：

```java
public interface PromptTemplateService {
    Long createPrompt(Long tenantId, PromptCreateParam param);
    CommonPage<PromptTemplateDto> listPrompts(Long tenantId, PromptQuery query, Integer pageSize, Integer pageNum);
    PromptTemplateDto getActivePrompt(Long tenantId, String promptCode);
    Long createVersion(Long tenantId, String promptCode, PromptVersionParam param);
    int enableVersion(Long tenantId, String promptCode, String version);
    int rollback(Long tenantId, String promptCode, String version);
    PromptRenderResult renderTest(Long tenantId, PromptRenderTestParam param);
}
```

## 11. Trace 与审计 API

### 11.1 TraceController

职责：查看 Agent 调用链、Span、工具调用、模型调用、审批和报告证据。

```text
GET    /api/traces/{traceId}
GET    /api/traces/{traceId}/spans
GET    /api/tasks/{taskId}/trace
GET    /api/tasks/{taskId}/tool-calls
GET    /api/tasks/{taskId}/model-calls
```

核心 Service：

```java
public interface TraceService {
    TraceDetailDto getTrace(Long tenantId, String traceId);
    List<TraceSpanDto> listSpans(Long tenantId, String traceId);
    TraceDetailDto getTaskTrace(Long tenantId, Long shopId, Long taskId);
    String createTraceId();
    String createSpan(TraceSpanCreateCommand command);
    int finishSpan(String traceId, String spanId, TraceSpanFinishCommand command);
}
```

### 11.2 AuditLogController

```text
GET    /api/audit/tool-call-logs
GET    /api/audit/model-call-logs
GET    /api/audit/task-events
GET    /api/audit/approval-actions
```

## 12. 企业级数据权限服务

### 12.1 PermissionService

```java
public interface PermissionService {
    boolean hasApiPermission(Long userId, String resourceCode);
    boolean hasToolPermission(Long tenantId, Long shopId, Long userId, String permissionCode);
    boolean hasShopAccess(Long tenantId, Long shopId, Long userId);
    DataScope getDataScope(Long tenantId, Long userId);
}
```

调用位置：

- Controller 入口做 API 权限。
- Service 查询前做租户和店铺权限。
- Tool Gateway 调用前做工具权限。
- ApprovalService 审批前做审批权限。

## 13. 每日经营复盘主链路

### 13.1 API 调用

```http
POST /api/agent/tasks
```

请求：

```json
{
  "taskType": "daily_review",
  "userInput": "帮我生成今天店铺运营复盘",
  "dateRange": {
    "start": "2026-07-18",
    "end": "2026-07-18"
  }
}
```

### 13.2 后端流程

```text
AgentTaskController.createTask
  -> AgentTaskService.createTask
     -> PermissionService.hasShopAccess
     -> insert agent_task CREATED
     -> insert agent_task_event
     -> AgentTaskSender.send taskId
  -> return taskId

AgentTaskReceiver.handle
  -> AgentEngineService.executeTask
     -> PlannerService.createPlan
     -> PlanValidator.validate
     -> ExecutorService.execute
        -> ToolGateway.invoke order.query_summary
        -> ToolGateway.invoke comment.query_negative
        -> ToolGateway.invoke product.query_candidates
        -> ToolGateway.invoke ad.query_performance
     -> ReportGenerationService.generateDailyReview
     -> VerifierService.verify
     -> OperationReportService.createReport
     -> update task SUCCESS / DEGRADED / FAILED
```

### 13.3 前端轮询

```text
GET /api/agent/tasks/{taskId}
GET /api/agent/tasks/{taskId}/steps
GET /api/tasks/{taskId}/trace
GET /api/reports/{reportId}
```

## 14. 高风险商品标题修改主链路

### 14.1 用户输入

```text
帮我优化这些低点击商品标题，并应用到店铺。
```

### 14.2 后端流程

```text
Planner 生成标题优化计划
Executor 调用 product.generate_title_candidates
Verifier 校验标题长度、禁用词、重复词、证据数据
Executor 尝试调用 product.update_title
ToolGateway 发现 risk_level = high 且 need_approval = true
ApprovalService.createApproval
任务进入 WAITING_APPROVAL
审批人查看候选标题、证据和 diff
审批通过
ApprovalService 调用 ToolGateway 执行 product.update_title
记录执行结果
任务继续完成
```

## 15. Controller 与 Service 实现建议

### 15.1 Controller 保持薄层

Controller 只负责：

- 参数接收。
- 参数校验。
- 获取当前用户、租户、店铺上下文。
- 调用 Service。
- 返回 CommonResult。

不在 Controller 写 Agent 编排、工具调用和审批逻辑。

### 15.2 Service 保持领域边界

建议：

- `AgentTaskService` 只管任务生命周期。
- `AgentEngineService` 只管编排执行。
- `ToolGatewayService` 只管工具调用入口。
- `McpToolService` 只管工具元数据。
- `ApprovalService` 只管审批流。
- `ReportService` 只管报告资产。
- `TraceService` 只管调用链。

### 15.3 事务边界

推荐事务边界：

- 创建任务：一个事务。
- 更新任务状态：一个短事务。
- 单次工具调用日志：一个短事务。
- 审批操作：一个事务。
- 报告创建：一个事务。

避免将整个 Agent 执行过程放在一个长事务中。

## 16. 第一版 API 优先级

P0：跑通每日经营复盘

```text
POST /api/agent/tasks
GET  /api/agent/tasks/{taskId}
GET  /api/agent/tasks/{taskId}/steps
GET  /api/reports/{reportId}
GET  /api/tools
POST /api/tools/{toolCode}/invoke
GET  /api/tasks/{taskId}/trace
```

P1：补齐工具治理与审批

```text
POST /api/tools
PUT  /api/tools/{toolCode}
GET  /api/approvals/pending
POST /api/approvals/{approvalId}/approve
POST /api/approvals/{approvalId}/reject
```

P2：企业管理能力

```text
TenantController
ShopController
ConnectorController
ModelProviderController
PromptTemplateController
AuditLogController
```

## 17. 总结

ShopOps 后端 API 的核心不是堆接口，而是形成稳定的企业级控制链路：

```text
用户请求
  -> 租户/店铺/API 权限
  -> Agent 任务
  -> 结构化计划
  -> 工具网关
  -> 工具权限/风险/Schema
  -> 工具执行或审批
  -> 调用链审计
  -> 报告沉淀
```

这个设计既能沿用 mall-master 的成熟 Spring Boot 后端风格，又能突出 ShopOps 作为企业级 Agent 运营中台的独立价值。
