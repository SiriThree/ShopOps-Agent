# ShopOps 每日经营复盘 P0 主链路实现设计

## 1. P0 目标

每日经营复盘是 ShopOps Agent 的第一条端到端主链路。P0 目标不是一次性实现全部企业级能力，而是用最小但完整的链路证明平台架构成立。

P0 需要跑通：

```text
用户创建任务
  -> 任务落库
  -> Planner 生成执行计划
  -> Executor 调用工具
  -> Tool Gateway 记录工具调用
  -> Report Service 生成报告
  -> Verifier 做基础校验
  -> 任务成功、降级或失败
  -> 前端可查看任务、步骤、报告和 Trace
```

P0 暂不强依赖真实淘宝、京东、拼多多接口，可以使用 mall-master 的 `oms_order`、`pms_product`、`pms_comment` 作为模拟电商数据源。

## 2. P0 范围

### 2.1 必做能力

- 创建每日经营复盘任务。
- 查询任务详情。
- 查询任务步骤。
- 查询工具调用日志。
- 生成运营复盘报告。
- 查看报告详情。
- 基础 Trace 串联。
- 任务失败时标记错误。
- 单个工具失败时允许降级生成报告。

### 2.2 暂缓能力

- 真实平台连接器。
- 复杂审批流。
- 多模型路由策略。
- DAG 并行执行。
- WebSocket 实时推送。
- 复杂 Prompt 版本管理。
- 企业级监控大盘。

P0 可以先用同步方法实现 Worker 逻辑，后续再接 RabbitMQ。

## 3. P0 API

### 3.1 创建任务

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

响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 10001,
    "taskNo": "TASK202607180001",
    "status": "CREATED",
    "traceId": "tr_20260718_xxxx"
  }
}
```

### 3.2 查询任务详情

```http
GET /api/agent/tasks/{taskId}
```

响应重点字段：

```json
{
  "taskId": 10001,
  "taskNo": "TASK202607180001",
  "taskType": "daily_review",
  "userInput": "帮我生成今天店铺运营复盘",
  "status": "SUCCESS",
  "traceId": "tr_20260718_xxxx",
  "reportId": 90001,
  "createdAt": "2026-07-18 10:00:00",
  "startedAt": "2026-07-18 10:00:01",
  "finishedAt": "2026-07-18 10:00:08"
}
```

### 3.3 查询任务步骤

```http
GET /api/agent/tasks/{taskId}/steps
```

### 3.4 查询报告详情

```http
GET /api/reports/{reportId}
```

### 3.5 查询 Trace

```http
GET /api/tasks/{taskId}/trace
```

P0 可以先返回：

```text
任务事件
工具调用日志
模型调用日志
报告生成信息
```

## 4. P0 数据表

P0 最少需要以下表：

```text
tenant
shop
shop_member
mcp_tool
agent_task
agent_task_step
agent_task_event
tool_call_log
operation_report
trace_span
```

如果第一阶段想更轻，可以先假设已有默认租户和默认店铺。

### 4.1 默认数据

建议初始化：

```text
tenant_id = 1
shop_id = 1
user_id = 1
```

默认工具：

```text
order.query_summary
comment.query_negative
product.query_candidates
report.generate_daily_review
```

## 5. P0 任务状态流转

每日经营复盘 P0 状态流转：

```text
CREATED
  -> PLANNING
  -> RUNNING
  -> SUCCESS
```

异常状态：

```text
PLANNING -> PLAN_FAILED
RUNNING  -> DEGRADED
RUNNING  -> FAILED
```

状态说明：

- `CREATED`：任务已创建，尚未执行。
- `PLANNING`：正在生成执行计划。
- `RUNNING`：正在调用工具和生成报告。
- `SUCCESS`：所有关键工具成功，报告生成成功。
- `DEGRADED`：部分工具失败，但报告已降级生成。
- `FAILED`：关键工具失败或报告生成失败。
- `PLAN_FAILED`：Planner 输出无效。

P0 暂不使用 `WAITING_APPROVAL`。

## 6. Planner 设计

P0 Planner 不建议完全依赖 LLM。可以先用规则 Planner，保证链路稳定。

输入：

```java
public class AgentTaskCreateParam {
    private String taskType;
    private String userInput;
    private DateRangeParam dateRange;
}
```

规则：

```text
taskType = daily_review
  -> 固定生成每日经营复盘计划
```

输出计划：

```json
{
  "task_type": "daily_review",
  "steps": [
    {
      "step_no": 1,
      "step_name": "查询订单核心指标",
      "tool_code": "order.query_summary"
    },
    {
      "step_no": 2,
      "step_name": "查询差评风险",
      "tool_code": "comment.query_negative"
    },
    {
      "step_no": 3,
      "step_name": "查询待优化商品",
      "tool_code": "product.query_candidates"
    },
    {
      "step_no": 4,
      "step_name": "生成经营复盘报告",
      "tool_code": "report.generate_daily_review"
    }
  ]
}
```

后续可以升级为：

```text
Rule Planner -> LLM Planner -> Hybrid Planner
```

## 7. P0 工具设计

### 7.1 order.query_summary

功能：查询指定时间范围内的订单核心指标。

输入 Schema：

```json
{
  "type": "object",
  "required": ["shopId", "startDate", "endDate"],
  "properties": {
    "shopId": {"type": "integer"},
    "startDate": {"type": "string", "format": "date"},
    "endDate": {"type": "string", "format": "date"}
  }
}
```

输出 Schema：

```json
{
  "type": "object",
  "required": ["gmv", "orderCount", "refundAmount", "refundRate", "avgOrderAmount"],
  "properties": {
    "gmv": {"type": "number"},
    "orderCount": {"type": "integer"},
    "refundAmount": {"type": "number"},
    "refundRate": {"type": "number"},
    "avgOrderAmount": {"type": "number"},
    "compareYesterday": {"type": "object"},
    "compareSevenDayAvg": {"type": "object"}
  }
}
```

实现类：

```text
OrderQuerySummaryExecutor
```

数据源：

```text
oms_order
oms_order_item
```

### 7.2 comment.query_negative

功能：查询低星评论和高风险关键词评论。

输入 Schema：

```json
{
  "type": "object",
  "required": ["shopId", "startDate", "endDate"],
  "properties": {
    "shopId": {"type": "integer"},
    "startDate": {"type": "string", "format": "date"},
    "endDate": {"type": "string", "format": "date"},
    "minStar": {"type": "integer", "default": 3}
  }
}
```

输出 Schema：

```json
{
  "type": "object",
  "required": ["negativeCount", "riskComments", "categoryStats"],
  "properties": {
    "negativeCount": {"type": "integer"},
    "riskComments": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "commentId": {"type": "integer"},
          "productId": {"type": "integer"},
          "star": {"type": "integer"},
          "content": {"type": "string"},
          "riskKeywords": {"type": "array", "items": {"type": "string"}}
        }
      }
    },
    "categoryStats": {"type": "object"}
  }
}
```

实现类：

```text
CommentQueryNegativeExecutor
```

数据源：

```text
pms_comment
```

### 7.3 product.query_candidates

功能：查询可能需要优化的商品。

P0 中 mall-master 原始商品表没有点击率、转化率等运营指标，可以先用模拟规则：

- 库存高但销量低。
- 新品状态但订单少。
- 评论差评较多。
- 标题长度不符合规则。

输入 Schema：

```json
{
  "type": "object",
  "required": ["shopId", "startDate", "endDate"],
  "properties": {
    "shopId": {"type": "integer"},
    "startDate": {"type": "string", "format": "date"},
    "endDate": {"type": "string", "format": "date"},
    "limit": {"type": "integer", "default": 10}
  }
}
```

输出 Schema：

```json
{
  "type": "object",
  "required": ["candidateCount", "products"],
  "properties": {
    "candidateCount": {"type": "integer"},
    "products": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "productId": {"type": "integer"},
          "productName": {"type": "string"},
          "reason": {"type": "string"},
          "score": {"type": "number"}
        }
      }
    }
  }
}
```

实现类：

```text
ProductQueryCandidatesExecutor
```

数据源：

```text
pms_product
oms_order_item
pms_comment
```

### 7.4 report.generate_daily_review

功能：根据前三个工具的结果生成结构化运营复盘报告。

P0 可先使用模板生成，不强依赖 LLM。

输入：

```json
{
  "orderSummary": {},
  "negativeComments": {},
  "productCandidates": {},
  "dateRange": {}
}
```

输出：

```json
{
  "title": "2026-07-18 店铺经营复盘",
  "markdown": "## 核心指标...",
  "summary": "今日 GMV ...",
  "evidence": {
    "toolCallIds": [1, 2, 3]
  }
}
```

实现类：

```text
DailyReviewReportExecutor
```

## 8. Tool Gateway P0 逻辑

P0 Tool Gateway 执行流程：

```text
1. 根据 tool_code 查询 mcp_tool
2. 检查 enabled
3. 检查用户是否有 permission_code
4. 检查 risk_level
5. 校验 input_schema
6. 生成 tool_call_log，状态 RUNNING
7. 查找 ToolExecutor
8. 执行工具
9. 校验 output_schema
10. 更新 tool_call_log 为 SUCCESS / FAILED
11. 返回 ToolInvokeResult
```

P0 可以暂缓：

- 限流。
- 熔断。
- 复杂重试。
- 审批。
- 外部连接器鉴权。

## 9. Executor P0 逻辑

P0 使用顺序执行，方便调试。

```text
1. 创建 step 1，调用 order.query_summary
2. 创建 step 2，调用 comment.query_negative
3. 创建 step 3，调用 product.query_candidates
4. 聚合前三步输出
5. 创建 step 4，调用 report.generate_daily_review
6. 保存 operation_report
```

失败策略：

- `order.query_summary` 失败：任务 `FAILED`，因为核心指标不可缺。
- `comment.query_negative` 失败：任务可 `DEGRADED`，报告标注评论数据缺失。
- `product.query_candidates` 失败：任务可 `DEGRADED`，报告标注商品优化数据缺失。
- `report.generate_daily_review` 失败：任务 `FAILED`。

## 10. Report P0 输出结构

Markdown 报告建议结构：

```markdown
# 店铺每日经营复盘

## 1. 核心指标

- GMV：
- 订单数：
- 退款金额：
- 退款率：
- 客单价：

## 2. 异常发现

- 退款率是否异常。
- 差评数量是否异常。
- 是否存在待优化商品。

## 3. 可能原因

- 基于订单、评论和商品数据说明。

## 4. 优化建议

- 商品优化建议。
- 评论处理建议。
- 运营关注事项。

## 5. 数据证据

- 订单工具调用 ID。
- 评论工具调用 ID。
- 商品工具调用 ID。

## 6. 数据缺失说明

- 如果发生降级，在这里说明。
```

## 11. Controller / Service / Mapper 清单

### 11.1 Controller

P0 必需：

```text
AgentTaskController
OperationReportController
TraceController
McpToolController
ToolInvokeController
```

### 11.2 Service

P0 必需：

```text
AgentTaskService
AgentEngineService
PlannerService
PlanValidator
ExecutorService
VerifierService
McpToolService
ToolGatewayService
ToolCallLogService
OperationReportService
TraceService
PermissionService
```

### 11.3 Tool Executor

P0 必需：

```text
OrderQuerySummaryExecutor
CommentQueryNegativeExecutor
ProductQueryCandidatesExecutor
DailyReviewReportExecutor
```

### 11.4 Mapper

P0 必需：

```text
AgentTaskMapper
AgentTaskStepMapper
AgentTaskEventMapper
McpToolMapper
ToolCallLogMapper
OperationReportMapper
TraceSpanMapper
```

复用 mall-master：

```text
OmsOrderMapper
OmsOrderItemMapper
PmsProductMapper
PmsCommentMapper
```

## 12. DTO 设计

### 12.1 AgentTaskCreateParam

```java
public class AgentTaskCreateParam {
    private String taskType;
    private String userInput;
    private DateRangeParam dateRange;
}
```

### 12.2 AgentTaskCreateResult

```java
public class AgentTaskCreateResult {
    private Long taskId;
    private String taskNo;
    private String status;
    private String traceId;
}
```

### 12.3 ToolInvokeContext

```java
public class ToolInvokeContext {
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private Long taskId;
    private Long stepId;
    private String traceId;
    private String parentSpanId;
    private Boolean manualInvoke;
}
```

### 12.4 ToolInvokeResult

```java
public class ToolInvokeResult {
    private Boolean success;
    private String status;
    private Object data;
    private Long toolCallLogId;
    private String errorCode;
    private String errorMessage;
}
```

### 12.5 AgentExecutionResult

```java
public class AgentExecutionResult {
    private Boolean success;
    private Boolean degraded;
    private Map<String, ToolInvokeResult> stepResults;
    private Long reportId;
    private String errorMessage;
}
```

## 13. Trace P0 设计

P0 至少生成这些 Span：

```text
agent.task.create
agent.planner
agent.executor
tool.order.query_summary
tool.comment.query_negative
tool.product.query_candidates
tool.report.generate_daily_review
agent.verifier
report.create
```

Trace 查询时聚合：

- `trace_span`
- `agent_task_event`
- `tool_call_log`
- `operation_report`

## 14. 初始化数据

### 14.1 mcp_tool

初始化四个 P0 工具：

```text
order.query_summary
comment.query_negative
product.query_candidates
report.generate_daily_review
```

工具配置建议：

```text
risk_level = low
need_approval = 0
enabled = 1
retry_count = 0
timeout_ms = 10000
```

### 14.2 权限

初始化权限：

```text
agent:task:create
agent:task:read
tool:invoke
order:read
comment:read
product:read
report:read
```

## 15. 编码顺序

建议按以下顺序开发：

1. 创建 P0 数据表 SQL。
2. 生成或手写 P0 Mapper / Model。
3. 实现 `McpToolService` 和工具初始化。
4. 实现 `ToolExecutor` 接口与 Registry。
5. 实现三个数据查询工具。
6. 实现 `ToolGatewayService`。
7. 实现 `AgentTaskService.createTask`。
8. 实现规则版 `PlannerService`。
9. 实现顺序版 `ExecutorService`。
10. 实现模板版 `DailyReviewReportExecutor`。
11. 实现 `OperationReportService`。
12. 实现 `VerifierService` 基础校验。
13. 实现 Trace 和任务事件查询。
14. 接入 RabbitMQ，将同步执行改为异步执行。

## 16. P0 验收标准

最小验收：

- 调用 `POST /api/agent/tasks` 能创建任务。
- 后端能自动生成 4 个步骤。
- 订单、评论、商品工具至少 2 个成功时可生成报告。
- 任务最终状态为 `SUCCESS` 或 `DEGRADED`。
- 报告能展示核心指标、异常发现、优化建议和数据证据。
- 能查询任务步骤。
- 能查询工具调用日志。
- 能通过 traceId 串起任务、步骤、工具和报告。

推荐验收样例：

```text
输入：帮我生成今天店铺运营复盘
输出：一份结构化 Markdown 报告
链路：agent_task -> agent_task_step -> tool_call_log -> operation_report -> trace_span
```

## 17. 后续演进

P0 跑通后，再升级：

- 从规则 Planner 升级为 LLM Planner。
- 从模板报告升级为 LLM 报告生成。
- 从顺序执行升级为并行/DAG 执行。
- 增加审批流支持商品标题修改。
- 增加广告投放工具。
- 增加模型网关和 Prompt 版本管理。
- 增加真实平台 Connector。
- 增加企业级限流、熔断和监控指标。

## 18. 总结

每日经营复盘 P0 是 ShopOps 的第一条主链路。它的价值不在于一次实现所有能力，而在于验证企业级 Agent 平台最核心的控制闭环：

```text
自然语言任务
  -> 结构化计划
  -> 工具网关
  -> 数据查询
  -> 报告生成
  -> 结果校验
  -> 调用链审计
```

只要这条链路跑通，后续的差评舆情、商品标题优化、投放复盘和审批执行都可以在同一套框架下扩展。
