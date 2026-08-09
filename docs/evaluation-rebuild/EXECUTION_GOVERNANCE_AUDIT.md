# ShopOps Phase 5 — Execution Governance Audit

## 1. Scope

本审计以 Phase 4 实际仓库代码为事实来源，目标是确认治理是否发生在真实执行边界，而不是由 Benchmark/Evaluator 推演安全结果。

主链路：

```text
HTTP / internal caller
→ RequestContext / trusted tenant-shop-user
→ AgentTask / AgentTaskContext（Agent 路径）
→ AuthorizationService.resolve(...)
→ ToolInvokeContext
→ DefaultToolGatewayService
→ trusted identity normalization
→ Tool schema validation
→ permission / risk / approval policy
→ ToolProvider
→ HighRiskRefundExecuteExecutor
→ WriteOperationService
→ RefundExternalClient
→ external system
```

Phase 5 高风险写主 Benchmark execution level 为 `TOOL_GATEWAY`。`order.refund_execute` 当前不在自然语言 Planner 可达集合，因此 TOOL_GATEWAY 结果不得描述成 LLM/Agent Governance。

## 2. Trusted Identity Source

### HTTP Bearer 路径

`RequestContextResolver` 解析认证身份，并通过 AuthorizationService 获得用户在 tenant/shop 中的授权事实。

### Header dev mode

仓库还存在仅在显式开发配置开启时可使用的 header identity 路径。它不是 Phase 5 正式 Governance 的可信授权来源，不应作为正式 RBAC 指标依据。

### Agent 路径

Phase 1 已使 `SequentialAgentExecutorService` 在调用工具前重新通过 `AuthorizationService.resolve(tenantId, shopId, userId)` 获取权限，而不是让模型提供权限集合。

### Tool Gateway 最终边界（Phase 5 修复）

审计发现 Phase 4 的 `DefaultToolGatewayService` 只检查 `ToolInvokeContext.permissions`，自身不会重新查询 AuthorizationService。这意味着内部调用者若能构造 ToolInvokeContext，就可能把伪造 permissions 当成执行事实。

Phase 5 修复后：

1. Gateway 要求 trusted `tenantId/shopId/userId` 存在；
2. Gateway 自己调用 `AuthorizationService.resolve(...)`；
3. caller permission snapshot 只能是 trusted permission 的子集，否则 `TOOL_PERMISSION_SNAPSHOT_MISMATCH`；
4. 实际执行权限最终替换为 trusted AuthorizationSnapshot；
5. Tool arguments 中的 tenant/shop/user/permissions/roles 不能提升或覆盖 trusted execution identity。

因此最终授权事实来源是 AuthorizationService，而不是 LLM/tool arguments，也不是调用者自行填充的 permission set。

## 3. Authorization Source

### JDBC

生产 JDBC 模式使用：

```text
JdbcAuthorizationService
→ AuthUserMapper.listAccessibleShopIds
→ JdbcUserRoleService
→ tenant_member / shop_member / user_account
```

真实角色归一化为 `VIEWER / OPERATOR / ADMIN`。`JdbcAuthorizationService` 再从角色生成 permission set。

关键权限事实：

- VIEWER：read permissions；
- OPERATOR：在 read 基础上包含 `order:refund` 等操作权限；
- ADMIN：进一步包含 approval review / connector management 等权限。

### Memory

`InMemoryAuthorizationService` 对有效 memory fixture 提供接近 ADMIN 的权限快照。因此 memory smoke 只能验证 identity/authorization propagation，不可作为正式细粒度 RBAC 结论。

Phase 5 的 deterministic Governance Dataset 使用独立 `GovernanceAuthorizationFixture`，明确标记：

```text
AUTHORIZATION_MODE = AUTHORIZATION_FIXTURE
```

正式 test 需要 JDBC authorization runtime。

## 4. Agent-supplied identity vs trusted execution identity

`TrustedToolInputNormalizer` Phase 5 明确阻断：

- arguments.shopId 与 trusted shopId 不一致 → `TOOL_SCOPE_MISMATCH`；
- arguments.tenantId / userId 与 trusted identity 不一致 → `TOOL_IDENTITY_ARGUMENT_CONFLICT`；
- arguments.permissions / roles → `TOOL_AUTHORIZATION_ARGUMENT_FORBIDDEN`；
- MCP 调用需要 shopId 时，由 trusted context 注入，而不是信任模型字段。

Identity conflict 不会 silent rewrite 后当合法请求计分；它被记录为真实 governance rejection。

## 5. Tool Metadata Runtime Enforcement

真实 `McpToolDto` 当前包含并被 Runtime 使用的字段：

| Metadata | Runtime enforcement |
|---|---|
| enabled | Gateway 拒绝 disabled tool |
| permissionCode | Gateway 对 trusted permissions 强制检查 |
| riskLevel | HIGH risk 强制进入 approval gate |
| needApproval | 与 risk policy 联合使用 |
| inputSchema | ToolInputSchemaValidator 在 provider 前执行 |
| providerType | provider selection |
| idempotent | 作为 catalog metadata；refund 的真实幂等保障主要由 WriteOperation/idempotency key 实现 |

当前 Domain 没有统一 `readOnly / reversible / approvalPolicy` 字段，因此 Benchmark 不伪造这些 metadata。

## 6. Schema Validation

Phase 5 继续复用真实 `McpToolDto.inputSchema`，没有 Benchmark-only schema。

`ToolInputSchemaValidator` 增强后可检查：

- object / array；
- required；
- additionalProperties=false；
- integer / number / string / boolean；
- enum；
- minimum / maximum；
- minLength / maxLength；
- date format。

`order.refund_execute` 的生产 tool schema 通过 `V24__phase5_execution_governance.sql` 收紧，验证发生在 Tool Gateway provider 之前。

## 7. Approval Binding

真实 Approval 已持久化/绑定：

- tenantId / shopId；
- requester；
- toolCode；
- canonical input hash；
- businessObjectId；
- risk level；
- source task/step/tool-call information。

`DefaultToolGatewayService.isApprovedForTool(...)` 比对：

```text
toolCode + canonicalized write input
```

其中 `approvalId` 不参与 canonical business payload，以避免执行 metadata 改变业务 payload identity。

Phase 5 修复：只有 `APPROVED` 状态能进入执行争抢；随后必须通过 `markExecuting` CAS/atomic transition 才能调用 provider。`EXECUTING/EXECUTED` approval 不再被当成可复用 approval。

Memory ApprovalService 也补齐与 JDBC 一致的：

```text
APPROVED → EXECUTING → EXECUTED
```

以及 execution failure 处理。

## 8. High-risk approval bypass audit

Phase 4 允许 shop runtime config `agent_tool_approval_enabled=false` 关闭通用 approval。对 HIGH risk refund 来说，这形成潜在绕过。

Phase 5 规则：

```text
HIGH risk => approval always enforced
```

普通可配置 approval 仍可保留现有 shop configuration 语义，但 HIGH risk 不可通过该通用开关绕过。

## 9. Tenant / Shop Persistence Boundary

已有 mapper/service 大量查询使用 tenant/shop scope。例如 BusinessOrderMapper 的聚合查询以 tenant/shop 作为过滤条件，Approval 与 WriteOperation 也保存 tenant/shop identity。

但本轮审计没有发现 `order.refund_execute` 在执行前通过 `orderId` 再查询订单归属并证明该 order 属于 trusted tenant/shop 的独立业务 ownership check。因此：

> “forged shopId / cross-shop context”可以由 Tool Gateway trusted scope 阻断，但“提供一个格式合法、来自其他 shop 的 orderId”这一更深层 business-object ownership invariant 当前不能被诚实宣称已由退款链完整验证。

这是 Phase 5 KNOWN LIMITATION，不在 Evaluator 中伪造 PASS。

## 10. Write Execution Gate / External Side Effect Boundary

高风险退款最终链：

```text
DefaultToolGatewayService
→ Approval state/binding
→ LocalToolProvider
→ HighRiskRefundExecuteExecutor
→ WriteOperationService
→ RefundExternalClient
→ RefundExternalTransport
```

Phase 5 Integration Benchmark 继续使用 Phase 3/4 `RecordingRefundExternalSystem` 作为独立 ground truth。它不读取本地 WriteOperation，因此：

```text
Tool returned BLOCKED
```

只有同时满足：

```text
external effect delta == 0
```

才能被视为正确阻断高风险非法写。

## 11. Audit conclusion

Phase 5 之前已经有多层治理，但最终 Gateway trusted authorization、HIGH-risk approval bypass 和 memory approval consumption 存在真实缺口。Phase 5 将这些规则下沉/对齐到真实生产执行边界。

当前仍需 JDBC/MySQL + Spring runtime 才能把 Governance 指标升级为正式结果。
