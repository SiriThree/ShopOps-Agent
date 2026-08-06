# ShopOps Agent P4 审计中心验收清单

本文档用于验收 P4 阶段已经启动的管理端审计中心能力。P4 当前目标不是新增底层审计表，而是把 P2/P3 已经沉淀的认证审计、任务事件、工具调用日志聚合成前端可直接使用的审计中心入口。

## 1. P4 当前能力范围

P4 当前覆盖：

- 管理端审计总览：`GET /api/admin/audit/overview`
- 管理端高风险操作聚合：`GET /api/admin/audit/high-risk`
- 管理端审计导出：`GET /api/admin/audit/export`
- 管理端统一审计时间线：`GET /api/admin/audit/timeline`
- 管理端审计事件详情：`GET /api/admin/audit/timeline/{source}/{resourceId}`
- 统一聚合来源：
  - `AUTH`：认证与授权审计事件
  - `TASK`：Agent 任务生命周期事件
  - `TOOL`：工具调用日志
- 统一事件字段：
  - `source`
  - `eventId`
  - `eventType`
  - `eventStatus`
  - `userId`
  - `username`
  - `taskId`
  - `traceId`
  - `toolCode`
  - `requestId`
  - `resourceType`
  - `resourceId`
  - `riskLevel`
  - `summary`
  - `detail`
  - `createdAt`

## 2. 审计总览接口

```http
GET /api/admin/audit/overview
```

返回字段：

```text
authEventTotal
authFailureTotal
taskEventTotal
taskFailureTotal
toolCallTotal
toolCallFailed
recentAuthEvents
recentTaskEvents
recentToolCalls
generatedAt
```

用途：

- 作为管理端审计中心首页入口。
- 快速查看认证、任务、工具三个审计维度的总量与失败量。
- 提供最近认证事件、任务事件、工具调用记录，方便一屏进入详情排查。

## 3. 统一审计时间线接口

## 3. 高风险操作聚合接口

```http
GET /api/admin/audit/high-risk
```

返回字段：

```text
total
elevatedRiskTotal
riskBreakdown
recentElevatedRiskEvents
generatedAt
```

用途：

- 作为安全与治理视图入口。
- 汇总 `HIGH / MEDIUM / LOW / UNKNOWN` 风险分布。
- 返回最近非低风险事件，当前包括认证失败、权限拒绝、任务失败、任务重试、任务重排，以及未来高风险工具调用。

## 4. 审计导出接口

```http
GET /api/admin/audit/export
```

支持与时间线一致的筛选参数：

```text
source
eventType
eventStatus
userId
username
taskId
traceId
toolCode
riskLevel
createdStart
createdEnd
```

返回结构：

```text
fileName
contentType
columns
rows
rowCount
generatedAt
```

预期：

- 默认返回最多 100 条导出行。
- `columns` 固定列顺序，便于前端直接生成 CSV/XLSX。
- `rows` 为扁平化审计字段，不直接展开复杂 detail。
- 当前 `contentType` 为 `text/csv`，表示导出用途。

## 5. 统一审计时间线接口

```http
GET /api/admin/audit/timeline
```

支持筛选：

```text
source
eventType
eventStatus
userId
username
taskId
traceId
toolCode
riskLevel
createdStart
createdEnd
pageNum
pageSize
```

### 5.1 按来源筛选

```http
GET /api/admin/audit/timeline?source=AUTH&pageNum=1&pageSize=20
GET /api/admin/audit/timeline?source=TASK&pageNum=1&pageSize=20
GET /api/admin/audit/timeline?source=TOOL&pageNum=1&pageSize=20
```

预期：

- `source=AUTH` 只返回认证审计事件。
- `source=TASK` 只返回任务事件。
- `source=TOOL` 只返回工具调用事件。

### 5.2 按状态筛选

```http
GET /api/admin/audit/timeline?eventStatus=FAILURE&pageNum=1&pageSize=20
```

预期：

- 认证失败、权限拒绝、任务失败、工具调用失败等失败类事件可以被统一查询。

### 5.3 按任务或 Trace 筛选

```http
GET /api/admin/audit/timeline?taskId={taskId}&pageNum=1&pageSize=20
GET /api/admin/audit/timeline?traceId={traceId}&pageNum=1&pageSize=20
```

预期：

- `taskId` 可定位某个 Agent 任务关联的任务事件与工具调用事件。
- `traceId` 可跨任务事件、工具调用记录追踪同一次执行链路。

### 5.4 按工具与风险等级筛选

```http
GET /api/admin/audit/timeline?source=TOOL&toolCode=product.query_candidates&pageNum=1&pageSize=20
GET /api/admin/audit/timeline?source=TOOL&riskLevel=low&pageNum=1&pageSize=20
```

预期：

- `toolCode` 可定位特定工具调用记录。
- `riskLevel` 可支持前端按风险等级高亮或筛选。
- 工具调用事件的 `resourceType` 为 `tool_call_log`，`resourceId` 为工具调用日志 ID。

## 6. 统一事件来源映射

| source | resourceType | resourceId | riskLevel 来源 |
| --- | --- | --- | --- |
| AUTH | auth_audit_event | auth audit event ID | 根据失败/拒绝类型推导 |
| TASK | agent_task | task ID | 根据任务事件类型推导 |
| TOOL | tool_call_log | tool call log ID | 优先来自日志，其次来自工具注册表 |

## 7. 审计事件详情接口

```http
GET /api/admin/audit/timeline/{source}/{resourceId}
```

路径参数：

```text
source: AUTH / TASK / TOOL
resourceId: 时间线事件返回的 resourceId
```

返回结构：

```text
event
resource
context
```

预期：

- `AUTH` 详情返回认证审计事件原始数据。
- `TASK` 详情返回任务详情聚合数据，包括 task、steps、events、report、spans、toolCalls。
- `TOOL` 详情返回工具调用日志，并在 context 中补充工具元数据和关联任务详情。
- 未找到事件时返回业务失败结果。

## 8. PowerShell 验收示例

启动后端后，可使用本地 Header 开发模式直接访问：

```powershell
$headers = @{
  "X-Tenant-Id" = "1"
  "X-Shop-Id" = "1"
  "X-User-Id" = "1"
  "X-User-Roles" = "ADMIN"
}

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/audit/overview" `
  -Headers $headers

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/audit/timeline?pageNum=1&pageSize=20" `
  -Headers $headers

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/audit/high-risk" `
  -Headers $headers

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/audit/export?source=TOOL&eventStatus=SUCCESS" `
  -Headers $headers

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/audit/timeline?source=TOOL&riskLevel=low&pageNum=1&pageSize=10" `
  -Headers $headers

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/audit/timeline/TOOL/{toolCallLogId}" `
  -Headers $headers
```

严格 Bearer 模式下应改用：

```powershell
$headers = @{
  Authorization = "Bearer $adminToken"
}
```

## 9. 自动化验收命令

常规测试：

```powershell
mvn -pl shopops-admin -am test
```

当前测试覆盖：

- 每日复盘任务创建。
- 工具调用日志生成。
- 审计总览接口返回任务事件和工具调用统计。
- 统一审计时间线返回 `TASK` 与 `TOOL` 来源事件。
- 工具时间线支持 `eventStatus=SUCCESS`。
- 工具时间线支持 `riskLevel=low`。
- 时间线事件包含 `resourceType` 与 `riskLevel`。
- 审计详情接口支持 `TASK` 与 `TOOL` 资源下钻。
- 高风险操作聚合接口返回风险分布与最近非低风险事件。
- 审计导出接口返回固定列定义与扁平化 rows。
- JDBC 模式下统一审计时间线使用数据库侧 `UNION ALL` 聚合 AUTH/TASK/TOOL 事件，并在 SQL 层完成筛选、排序、分页与 total 统计。

## 10. P4 当前结论

P4 已经从分散的审计接口推进到统一审计中心入口：

- 总览接口可支撑审计中心首页。
- 高风险接口可支撑安全治理入口。
- 导出接口可支撑审计数据下载前的数据准备。
- 时间线接口可支撑审计列表页。
- 详情接口可支撑审计事件下钻页。
- 事件来源、资源定位、风险等级已经标准化。
- 当前实现复用已有服务和已有表结构；JDBC 模式下已经下推为数据库侧 union 查询，memory 模式保留原有内存聚合路径。

## 11. 下一阶段建议

后续可以继续扩展：

```text
1. 前端管理台对接审计中心页面
2. 如需真实文件下载，再补 CSV/XLSX 流式导出接口
```
