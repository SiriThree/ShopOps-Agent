# ShopOps Agent P2 管理端 API 与验收清单

## 1. 阶段定位

P2 的目标不是继续扩展 Agent 编排链路，而是把 P1 已经打通的真实业务数据、任务执行、报告生成、工具调用、事件审计能力，整理成企业后台可观测、可排障、可展示的一组管理端 API。

这一阶段完成后，ShopOps 后端已经具备一个企业级 AgentOps 后台的核心观测闭环：

```text
任务列表 -> 任务详情 -> 步骤/工具调用 -> Trace -> 事件审计 -> 报告管理 -> Dashboard 汇总
```

## 2. 已完成提交

```text
5297a10 feat: add admin task detail metrics api
d251845 feat: enhance admin task list filters
80b94fd feat: add admin task event audit stream
774f4ce feat: enhance operation report management api
eb3fa72 feat: add admin dashboard summary api
```

## 3. 管理端任务 API

### 3.1 任务列表

```http
GET /api/admin/agent/tasks
```

支持筛选：

```text
status
taskType
taskNo
userId
traceId
reportId
createdStart
createdEnd
finishedStart
finishedEnd
pageNum
pageSize
```

用途：

- 后台任务列表页。
- 通过任务编号、Trace ID、报告 ID 定位一次 Agent 执行。
- 排查失败、运行中、排队中任务。

### 3.2 任务详情

```http
GET /api/admin/agent/tasks/{taskId}/detail
```

聚合返回：

```text
task
steps
events
report
spans
toolCalls
```

用途：

- 后台任务详情页一屏展示。
- 从任务直接下钻到工具调用、Trace、报告、事件流。

### 3.3 任务指标

```http
GET /api/admin/agent/tasks/metrics
```

返回：

```text
total
created
queued
running
success
failed
degraded
successRate
avgLatencyMs
statusBreakdown
```

用途：

- 管理后台任务健康指标。
- Dashboard 任务成功率与状态分布。

## 4. 任务事件审计 API

```http
GET /api/admin/agent/tasks/events
```

支持筛选：

```text
taskId
eventType
fromStatus
toStatus
operatorId
createdStart
createdEnd
pageNum
pageSize
```

事件类型当前包括：

```text
TASK_CREATED
TASK_QUEUED
TASK_STARTED
TASK_FINISHED
TASK_FAILED
TASK_RETRY_REQUESTED
TASK_REQUEUED
```

事件结构化数据 `eventData` 当前包括：

```text
taskNo
taskType
traceId
reportId
errorCode
errorMessage
eventType
```

用途：

- 审计“谁在什么时间触发了什么任务操作”。
- 排查任务状态流转。
- 支持后台事件流页面和失败事件列表。

## 5. 报告管理 API

### 5.1 报告列表

```http
GET /api/reports
```

支持筛选：

```text
taskId
reportNo
reportType
traceId
status
createdBy
createdStart
createdEnd
pageNum
pageSize
```

用途：

- 报告中心列表页。
- 从任务 ID、Trace ID 反查报告。
- 统计报告生成数量。

### 5.2 报告详情

```http
GET /api/reports/{reportId}
```

返回字段包括：

```text
reportId
tenantId
shopId
taskId
reportNo
reportType
title
markdown
evidence
traceId
status
createdBy
createdAt
updatedAt
```

用途：

- 展示每日经营复盘报告。
- 查看报告证据链，例如风险评价 ID、商品 ID 等。

## 6. Dashboard 汇总 API

```http
GET /api/admin/dashboard/summary
```

返回：

```text
taskMetrics
reportTotal
toolCallTotal
toolCallFailed
recentFailedEvents
generatedAt
```

用途：

- 管理后台首页。
- 快速展示 Agent 任务健康度、报告产出、工具调用情况和最近失败事件。
- 面试展示时可以作为“企业级后台观测能力”的入口。

## 7. PowerShell 验证示例

启动 JDBC 模式：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--shopops.persistence=jdbc"
```

创建每日复盘任务：

```powershell
$body = @{
  taskType = "daily_review"
  userInput = "帮我生成今天店铺运营复盘"
  dateRange = @{
    start = "2026-07-18"
    end = "2026-07-18"
  }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/agent/tasks" `
  -Headers @{
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
  } `
  -ContentType "application/json; charset=utf-8" `
  -Body $body
```

常用管理端验证：

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/admin/agent/tasks?pageNum=1&pageSize=10"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/admin/agent/tasks/{taskId}/detail"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/admin/agent/tasks/metrics"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/admin/agent/tasks/events?taskId={taskId}"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/reports?pageNum=1&pageSize=10"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/admin/dashboard/summary"
```

## 8. 自动化验收命令

默认 memory 测试：

```powershell
mvn -pl shopops-admin -am test
```

JDBC 集成测试：

```powershell
mvn -pl shopops-admin test "-Dshopops.jdbc.it=true" "-Dtest=AgentTaskJdbcFlowIntegrationTest,AgentTaskJdbcFailureIntegrationTest"
```

完整构建：

```powershell
mvn clean install
```

当前 P2 每个增量提交均已通过以上命令。

## 9. P2 完成度

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 管理端任务列表 | 完成 | 支持多条件筛选与分页 |
| 管理端任务详情 | 完成 | 聚合 task、steps、events、report、trace、toolCalls |
| 任务指标 | 完成 | 支持状态分布、成功率、平均耗时 |
| 事件审计流 | 完成 | 支持分页筛选与结构化 eventData |
| 报告管理 | 完成 | 支持列表、详情、筛选、租户店铺隔离 |
| Dashboard 汇总 | 完成 | 聚合任务、报告、工具调用、失败事件 |
| JDBC 集成测试 | 完成 | 覆盖主链路、失败链路、P2 管理端接口 |

## 10. 现阶段可对外讲法

ShopOps Agent 后端已经不只是一个“能调用工具生成报告”的学生项目骨架，而是具备了企业后台常见的可观测能力：

- 任务全生命周期管理。
- 工具调用与 Trace 追踪。
- 事件审计与重试记录。
- 报告中心。
- Dashboard 汇总指标。
- MySQL / Redis / RabbitMQ / Flyway / MyBatis 的真实后端工程链路。

## 11. 下一阶段建议

P3 建议进入“企业治理与安全边界”：

```text
1. 管理端鉴权与角色权限
2. 租户/店铺权限校验强化
3. 工具风险等级与审批流
4. 操作审计日志标准化
5. OpenAPI / Swagger 文档完善
6. 前端管理台页面对接
```

其中最适合作为下一步后端增量的是：

```text
P3-1: 管理端鉴权与用户上下文标准化
```

因为当前接口已经通过 `X-Tenant-Id / X-Shop-Id / X-User-Id` 表达上下文，下一步可以自然升级为登录态、角色、权限和租户隔离策略。
