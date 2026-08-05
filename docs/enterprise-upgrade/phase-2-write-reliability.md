# ShopOps 企业级改造阶段 2：写操作、审批、事务、幂等与补偿

- 执行日期：2026-08-05
- 输入基线：阶段 1 仓库
- 旗舰链路：`order.refund_execute`
- 阶段边界：未扩张业务域，未重构前端视觉，未增强 Agent 自主性

## 1. 阶段审计结论

现有退款工具已经接入 Tool Gateway、权限检查、HIGH 风险审批、工具调用日志和 Trace，但执行器只生成随机退款号，没有真实业务状态、数据库幂等、审批参数一致性、外部结果未知处理、Outbox 或对账。审批只能从 PENDING 转为 APPROVED/REJECTED/WITHDRAWN/EXPIRED；通过后可重复携带 approvalId 调用，且原实现只校验工具代码，不校验批准参数与执行参数一致。RabbitMQ 发布仍存在数据库提交与消息发送之间的丢失窗口。

因此选择退款执行作为旗舰写操作：它是当前唯一明确标记为外部高风险副作用的工具，能够在不新增业务域的前提下覆盖审批、幂等、状态机、外部回查、事务 Outbox 和人工对账语义。当前外部退款客户端仍是可控模拟适配器，不能宣称已接入真实电商退款 API。

## 2. 全部写操作盘点

运行时工具元数据继续来自 `mcp_tool`，`risk_level`、`need_approval`、`idempotent`、`retry_count`、`permission_code` 已参与 Gateway 决策。当前主要写操作如下：

| 操作 | readOnly | riskLevel | idempotent | reversible | externalSideEffect | permission | approvalPolicy | retryPolicy |
|---|---:|---|---:|---:|---:|---|---|---|
| `order.refund_execute` | 否 | HIGH | 是，V20 强制 | 否 | 是，当前为模拟外部客户端 | `order:refund` | 强制审批 | 外部未知禁止重调，先回查 |
| `product.update_title` | 否 | HIGH | 元数据为是，但执行器仍偏演示 | 视外部平台而定 | 当前主要返回模拟结果 | `product:update` | 强制审批 | 尚未接入本阶段状态机 |
| `ad.suggest_budget` | 建议性质，当前无真实写入 | HIGH | 元数据为是 | 可重新调整 | 当前没有真实平台写入 | 工具注册值 | 强制审批 | 无真实外部重试 |
| `feishu.sync_report` | 否 | MEDIUM | 元数据声明是 | 否 | 是，Webhook | 工具注册值 | 按租户策略 | 当前仍直接调用，未接入旗舰状态机 |
| `report.export_excel` | 否 | LOW | 元数据声明是 | 可覆盖/删除文件 | 本地文件副作用 | 工具注册值 | 自动 | 文件写失败可重新生成 |
| 审批决策/撤销/过期 | 否 | MEDIUM/HIGH | CAS 条件更新 | 否 | 否 | `approval:review` | 不适用 | 不自动重试冲突 |
| Agent 任务创建/重试/取消 | 否 | MEDIUM | 部分状态条件保护 | 部分 | RabbitMQ 可产生副作用 | `agent:execute`/`task:cancel` | 由工具风险决定 | 阶段 2 未全面重构 |
| Connector 凭据和同步任务管理 | 否 | HIGH | 部分 | 部分 | 可能外部调用 | `connector:manage` | 现有权限控制 | 阶段 2 未全面接入状态机 |

限制：除退款旗舰链路外，其他写操作仍未全部迁移到统一 `write_operation`。因此不能宣称“所有平台写操作均已具备完整幂等与补偿”。

## 3. 实际完成范围

1. 新增 `write_operation`，以数据库唯一键保存业务幂等记录、输入摘要、审批绑定、状态、外部引用、结果和恢复动作。
2. 新增 `outbox_event`，业务确认事务中写入待发布事件，并记录状态、尝试次数、下次重试时间和最后错误。
3. `order.refund_execute` 强制为 HIGH、强制审批、禁止自动重试，并启用数据库幂等。
4. 审批参数使用规范化 JSON 和 SHA-256 摘要绑定；重试时仅排除传输字段 `approvalId`，实质参数变化会被拒绝。
5. 审批增加执行状态：`EXECUTING`、`EXECUTED`、`EXECUTION_FAILED`；CAS 更新防止并发重复执行。
6. 写操作状态机实现合法转换校验，包括 `EXTERNAL_UNKNOWN` 和 `NEEDS_RECONCILIATION`。
7. 幂等键为 `toolCode + tenantId + shopId + businessObjectId + operationRequestId`，数据库唯一约束是最终防线。
8. 相同幂等键和相同参数返回既有成功结果；相同键但参数变化拒绝；执行中请求拒绝并发重复调用。
9. 外部明确失败进入 `FAILED`；外部超时但可能已成功进入 `EXTERNAL_UNKNOWN`，禁止直接重新执行原操作。
10. 外部调用不位于数据库事务中；外部成功后的本地确认、最终状态和 Outbox 写入由事务服务处理。
11. 新增对账服务，通过外部 reference 回查未知退款状态，确认后完成本地状态和 Outbox。
12. 新增 Outbox 发布入口；RabbitMQ 发布失败保留 PENDING 并指数退避，可安全再次发布。
13. 新增写操作状态机单元测试代码。

## 4. 未完成范围

- 外部退款客户端是确定性模拟适配器，不是真实电商平台 API；真实连接器必须实现供应商幂等键、查询接口和错误分类。
- `product.update_title`、Feishu Webhook、Connector 管理等其他写操作尚未全部迁移到统一状态机。
- Outbox 当前提供显式发布入口，尚未启用自动调度、publisher confirm/return 或独立 Outbox Worker。
- Outbox 多实例 claim 字段已预留，但本阶段发布查询尚未实现 `SKIP LOCKED`/lease claim；因此不能宣称多实例发布器完整完工。
- `NEEDS_RECONCILIATION` 已定义，但当前退款回查成功路径主要从 `EXTERNAL_UNKNOWN` 恢复。
- 审批创建和执行审计继续复用现有工具日志、审批表与 Trace；尚未新增独立不可变审计事件表。
- 本阶段新增集成场景因 Maven 缺失未能执行，无法证明验收测试已经通过。

## 5. 修改文件清单

- `ShopOpsAdminApplication.java`
- `DefaultToolGatewayService.java`
- `HighRiskRefundExecuteExecutor.java`
- `ApprovalStatus.java`
- `ApprovalRequestCreateParam.java`
- `ApprovalRequestService.java`
- `JdbcApprovalRequestService.java`
- `ApprovalRequest.java`
- `ApprovalRequestMapper.java`

## 6. 新增文件清单

- `V20__phase2_write_reliability.sql`
- `reliability/domain/WriteOperation.java`
- `reliability/domain/WriteOperationStatus.java`
- `reliability/persistence/WriteOperationMapper.java`
- `reliability/persistence/OutboxEventMapper.java`
- `reliability/service/WriteOperationService.java`
- `reliability/service/RefundExternalClient.java`
- `reliability/service/OutboxPublisher.java`
- `reliability/service/WriteOperationReconciliationService.java`
- `reliability/controller/WriteReliabilityController.java`
- `WriteOperationStatusTest.java`
- 本交接文档

## 7. 删除文件清单

无。

## 8. 核心设计说明

```text
Tool Gateway
→ permission + risk + forced approval
→ canonical input/hash matches approved payload
→ approval APPROVED -> EXECUTING (CAS)
→ write_operation unique idempotency record
→ EXECUTING
→ external call outside DB transaction
   ├─ explicit failure -> FAILED
   ├─ timeout/unknown -> EXTERNAL_UNKNOWN -> query/reconcile
   └─ success -> EXTERNAL_SUCCEEDED
→ local transaction: LOCAL_CONFIRMED -> SUCCEEDED + Outbox
→ approval EXECUTED / EXECUTION_FAILED
→ Tool Log + Trace
```

Agent 计划不授予权限，也不改变租户、店铺、审批或幂等语义。Gateway 是执行裁决点，数据库唯一约束和状态 CAS 是并发最终防线。

## 9. 数据库变化

Flyway V20：

- `approval_request` 增加 `input_hash`、`business_object_id`、执行开始/完成时间；
- 新建 `write_operation`，`idempotency_key` 唯一；
- 新建 `outbox_event`；
- 更新 `order.refund_execute` 元数据为 HIGH、强制审批、幂等、零自动重试。

迁移尚未在真实 MySQL 上执行，必须验证 JSON 列、索引名称和已有 V1—V19 数据兼容性。

## 10. 配置变化

无新增密钥或危险默认配置。Outbox RabbitMQ 发布仅在 `shopops.agent.dispatch-mode=rabbitmq` 时创建 Publisher Bean。

## 11. 已执行测试及结果

| 命令 | 结果 |
|---|---|
| `mvn test` | 未执行：`mvn` 不存在，退出码 127 |
| `mvn package -DskipTests` | 未执行：`mvn` 不存在，退出码 127 |
| `npm ci` | 失败：内部 npm registry 缺少 `zrender-5.6.1.tgz` |
| `npm run typecheck` | 失败：没有 `typecheck` script |
| `npm run build` | 失败：TypeScript 在 `src/users.tsx` 报多处隐式 `any`；属于既有前端基线问题 |
| `docker compose ... config` | 未执行：`docker` 不存在，退出码 127 |
| Java 源码花括号静态检查 | 未发现不平衡文件；不等同于编译通过 |

新增测试代码未被表述为已通过。

## 12. 未能执行的验证

需要 Maven 3.9+、MySQL 8、RabbitMQ、可用 npm registry 和 Docker。必须执行：Flyway V1—V20 全新迁移及升级迁移、并发审批、并发幂等调用、事务回滚、RabbitMQ 重复消息、Outbox 发布失败恢复、外部超时后回查、原有审批/工具/Agent 集成回归。

## 13. 已知风险

- P1：新增 Java 未经 Maven 编译，可能存在依赖或 MyBatis 映射问题。
- P1：Outbox 尚无多实例 claim/lease 和 publisher confirm，仍不能宣称完整可靠发布。
- P1：除退款外的真实副作用工具尚未统一治理。
- P1：模拟退款回查总是返回成功，只用于验证控制流，不代表真实外部一致性。
- P2：前端构建存在既有 TypeScript 隐式 any 错误。
- P2：审批 `EXECUTION_FAILED` 后的人工重新审批/重新执行流程尚未提供专用管理 API。

## 14. 下一阶段依赖与交接结论

阶段 3 前必须先在完整环境中完成阶段 1 安全回归和本阶段可靠性集成测试。尤其需要证明数据库唯一约束、审批 CAS、外部未知禁止重调、Outbox 事务写入及对账恢复真实通过。

本阶段完成了退款旗舰写操作的代码级可靠性闭环，但因后端无法编译测试、外部客户端仍为模拟实现、Outbox 多实例发布未完工，不能宣称整个平台所有写操作已达到生产级。阶段 2 到此停止，不自动进入阶段 3。
