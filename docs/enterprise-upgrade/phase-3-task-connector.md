# 阶段 3：异步任务平台与生产级连接器

执行日期：2026-08-05

## 1. 阶段审计结论

### 1.1 原任务链路

```text
Task Persist(PENDING/原 CREATED)
→ 按 dispatch-mode 同步执行或 RabbitMQ 发布
→ RabbitAgentTaskConsumer
→ JdbcAgentTaskExecutionWorker
→ 仅用 QUEUED→RUNNING 条件更新防并发
→ AgentEngine
→ SUCCESS/FAILED/DEGRADED
```

原实现有 RabbitMQ 主队列和 DLX 配置，但没有 Worker 租约、心跳、attempt 解释、统一错误分类和真正的死信管理入口。`requeueStaleTasks` 仅依据 created_at/started_at 判断并重新发消息，无法区分仍存活的长任务与崩溃 Worker。

### 1.2 原连接器链路

```text
Connector Config/Credential
→ ConnectorSyncJob(status=RUNNING)
→ ConnectorSyncJobExecutor
→ 仅调用 ConnectorStatusService 检查文件是否存在
→ SUCCESS/FAILED
```

原同步任务未读取业务数据，不支持分页、游标、断点、去重、Schema 校验或幂等写入。因此阶段 2 以前不能宣称连接器同步已形成生产闭环。

## 2. 实际完成范围

### 2.1 统一任务状态

AgentTaskStatus 调整为：

```text
PENDING
QUEUED
RUNNING
WAITING_APPROVAL
RETRYING
SUCCEEDED
FAILED
CANCEL_REQUESTED
CANCELLED
NEEDS_MANUAL_ACTION
```

状态枚举显式声明合法转换和终态。历史 `CREATED/SUCCESS/DEGRADED` 在 V21 中迁移为 `PENDING/SUCCEEDED/NEEDS_MANUAL_ACTION`。

### 2.2 Worker 租约

`agent_task` 新增：

- worker_id
- locked_at
- lease_expire_at
- heartbeat_at
- attempt/max_attempts
- error_type/status_reason
- cancel_requested_at

Worker 使用单条条件 UPDATE 原子获取租约。只有状态为 QUEUED/RETRYING、租约不存在或已过期、且未请求取消的任务可以进入 RUNNING。重复 RabbitMQ 消息无法同时获得租约。

增加 heartbeat Mapper 操作和基于 lease_expire_at 的过期识别基础。当前 AgentEngine 为单次阻塞调用，尚未接入周期性 heartbeat 调度，见未完成范围。

### 2.3 错误分类

新增 TaskErrorType 和 TaskErrorClassifier：

- VALIDATION_ERROR
- PERMISSION_DENIED
- BUSINESS_CONFLICT
- RATE_LIMITED
- NETWORK_TIMEOUT
- DEPENDENCY_UNAVAILABLE
- EXTERNAL_RESULT_UNKNOWN
- INTERNAL_ERROR

元数据包含 retryable、maxAttempts、requiresLookup 和 manualAfterFailure。Worker 已保存错误分类；外部结果未知和需人工处理的错误进入 NEEDS_MANUAL_ACTION，而不是盲目重新执行。

### 2.4 任务取消

新增任务取消 API：

```text
POST /api/agent/tasks/{taskId}/cancel
```

取消采用 tenant/shop 条件更新并写 TaskEvent。任务进入 CANCEL_REQUESTED。已完成的不可逆步骤不会被伪装成已回滚。

### 2.5 深度连接器选择

选择 `file.order-summary`，原因：

1. 现有仓库已经有文件路径解析、连接状态、凭据和调用日志；
2. 不需要伪造真实商业平台接入；
3. 可真实验证分页、游标、断点、去重和幂等落库语义；
4. 复用现有 ConnectorSyncJob，不建立平行系统。

新增实际同步行为：

```text
Connector Status
→ 读取订单汇总文件
→ 以 100 行分页
→ cursorValue 定位下一页
→ 提取外部 ID
→ SHA-256 payload hash
→ connector_sync_item 唯一键 UPSERT
→ 保存 checkpoint/next cursor
→ API call log
```

唯一约束：

```text
tenant_id + shop_id + connector_code + external_type + external_id
```

重复同步不会新增重复业务项；内容变化会更新 hash、payload 和 last_seen_at。

## 3. 未完成范围

以下能力尚未完成，不能按生产完成声明：

1. heartbeat Mapper 已有，但长任务执行期间尚未启动周期心跳线程；超过 5 分钟的真实长任务可能被误判租约过期。
2. 统一错误策略元数据已建立，但 RATE_LIMITED/DEPENDENCY_UNAVAILABLE 的自动指数退避、next retry 调度和重新发布尚未接入完整 Worker。
3. RabbitMQ DLX 已存在，但没有 DLQ 列表、人工重放、attempt 详情页面。
4. WAITING_APPROVAL 与审批恢复尚未统一进入同一任务状态机；现有 Tool/Approval 链路仍主要在 AgentEngine 内部处理。
5. CANCEL_REQUESTED 能阻止尚未取租约的任务，但运行中的 AgentEngine 尚未在每个步骤前轮询取消标记。
6. `file.order-summary` 已实现文件分页和数据库幂等 staging，但没有正式映射到 order 业务表；这是为了避免在本阶段无依据覆盖订单主数据。
7. CSV Schema 目前仅做基本行和外部 ID 检查，尚未实现严格列 Schema 版本治理。
8. 文件连接器不需要 API 429；因此 429 分类已建立，但未在该连接器中制造虚假远程调用来宣称退避测试通过。
9. connector_sync_job 尚未使用原子租约领取，多实例同步同一 job 的治理仍需补齐。
10. 凭据加密、掩码、禁用和审计沿用阶段 0—2 实现，本阶段没有证明密钥轮换流程通过集成测试。

## 4. 修改文件清单

- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/domain/AgentTaskStatus.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/AgentTaskService.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/controller/AgentTaskController.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/JdbcAgentTaskService.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/InMemoryAgentTaskService.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/JdbcAgentTaskExecutionWorker.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/DefaultAgentTaskAdminService.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/persistence/model/AgentTask.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/persistence/mapper/AgentTaskMapper.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/connector/service/impl/ConnectorSyncJobExecutor.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/connector/service/impl/JdbcConnectorSyncJobService.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/persistence/model/ConnectorSyncJob.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/persistence/mapper/ConnectorSyncJobMapper.java`

## 5. 新增文件清单

- `shopops-admin/src/main/resources/db/migration/V21__phase3_task_connector.sql`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/reliability/TaskErrorType.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/reliability/TaskErrorClassifier.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/persistence/model/ConnectorSyncItem.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/persistence/mapper/ConnectorSyncItemMapper.java`
- `shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/reliability/TaskErrorClassifierTest.java`
- `shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/reliability/AgentTaskStatusTest.java`
- `docs/enterprise-upgrade/phase-3-task-connector.md`

## 6. 删除文件清单

无。

## 7. 核心设计说明

- 数据库租约而非 JVM 锁用于防重复执行，多实例语义明确。
- RabbitMQ 消息仅是唤醒信号，数据库任务记录才是状态事实来源。
- 重复消息通过租约 CAS 变为无副作用返回。
- 不把所有异常统一重试；结果未知必须先回查或人工处理。
- 连接器分页只有整页数据全部 UPSERT 成功后才保存下一游标。当前 `run + updateResult` 未加 `@Transactional`，因此进程在二者之间崩溃会重复读取该页，但唯一键 UPSERT 保证不重复创建 staging item。
- 没有直接覆盖订单主表，以免文件 Schema 不稳定时污染核心业务数据。

## 8. 数据库变化

Flyway V21：

- 扩展 agent_task 的租约、错误和取消字段；
- 扩展 connector_sync_job 的 cursor/checkpoint/error/retry/lease 字段；
- 新建 connector_sync_item 幂等同步 staging 表；
- 迁移历史 Agent 任务状态名称。

未执行数据库迁移，原因是当前环境没有可用 MySQL/Maven/Docker。

## 9. 配置变化

无新增生产密钥或危险默认配置。任务租约当前代码常量为 5 分钟，后续应转为带安全默认值的配置项。

## 10. 已执行测试及结果

### 静态检查

- Java 文件花括号平衡检查：未发现不平衡文件。
- 新增 JUnit：TaskErrorClassifierTest、AgentTaskStatusTest。
- 由于 Maven 不可用，这些测试未真实运行，不能标记 PASS。

### 命令结果

| 命令 | 结果 |
|---|---|
| `mvn test` | 未执行成功，`mvn: command not found`，退出码 127 |
| `mvn package -DskipTests` | 未执行成功，`mvn: command not found`，退出码 127 |
| Maven 安装尝试 | `apt-get update/install maven` 在 120 秒内超时，Maven 未安装 |
| `npm run build` | 失败；node_modules 不完整，缺少 vite、React、Ant Design 等模块 |
| `docker compose -f deploy/docker-compose.yml config` | 未执行成功，`docker: command not found`，退出码 127 |

## 11. 未能执行的验证

- Worker 崩溃后的真实租约接管；
- RabbitMQ 重复消息集成测试；
- MySQL CAS 并发领取；
- Flyway V21 实际迁移；
- 分页文件同步的数据库 UPSERT；
- 中途崩溃后游标续传；
- 多实例 connector job 并发；
- 前端任务中心回归。

所需环境：JDK 17、Maven 3.9+、MySQL 8、RabbitMQ、完整 npm registry 或已安装 node_modules、Docker Compose。

## 12. 已知风险

- P1：长任务未周期心跳，5 分钟租约可能过期并被另一 Worker 接管。
- P1：自动重试调度和 RabbitMQ publisher confirm 未闭环。
- P1：Connector Job 自身尚未 CAS 领取。
- P2：文件 Schema 版本和列级校验不足。
- P2：DLQ 缺少管理和安全重放接口。
- P2：取消尚未逐步骤协作式生效。
- P3：前端尚未展示 worker、lease、errorType、statusReason、cursor/checkpoint。

## 13. 下一阶段依赖

在进入阶段 4 前，应先在完整环境中修复编译问题并完成：

1. V21 Flyway 迁移验证；
2. Worker lease/heartbeat/Testcontainers 集成测试；
3. RabbitMQ 重复消息与 DLQ 测试；
4. Connector 分页、重复导入、断点续传测试；
5. 将 heartbeat 和 connector job lease 补齐后再宣称异步任务可恢复。

## 14. 简历声明边界

当前可以谨慎描述：

> 为 ShopOps 异步 Agent 任务引入数据库租约、任务状态转换、错误分类和取消请求；为订单汇总文件连接器实现分页游标、SHA-256 内容指纹、数据库唯一键去重和断点检查点。

当前不能宣称：

- 已完成生产级 RabbitMQ 自动重试与 DLQ 平台；
- Worker 崩溃恢复已通过真实集成测试；
- 连接器已对接真实电商商业 API；
- 429 指数退避、并发限制和外部写操作回查全部完成；
- 所有异步任务和连接器测试已通过。
