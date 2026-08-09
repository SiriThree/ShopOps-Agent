# Phase 5 Governance Architecture

## 1. Objective

Execution Governance Benchmark 不测试“模型是否自觉安全”，而测试真实执行边界能否阻止非法业务副作用，同时保留合法操作。

```text
GovernanceCase
→ trusted authorization fixture / JDBC AuthorizationService
→ real DefaultToolGatewayService
→ trusted identity normalization
→ real schema validator
→ permission / risk / approval policy
→ real ToolProvider / WriteOperation
→ independent external ground truth
→ CollectedEvidence
→ ExecutionGovernanceEvaluator
→ EvaluationRecord
→ GovernanceMetricsAggregator
→ BenchmarkReportWriter
```

## 2. Execution levels

- `TOOL_GATEWAY`：Phase 5 高风险主线；
- `AGENT`：仅未来 NL Agent 真能规划高风险工具时使用；
- `HTTP`：可做接口身份回归，但不与 Tool Gateway 主指标混算；
- `WRITE_EXECUTOR`：用于底层可靠性/安全回归，不能升级成 Tool Gateway Governance 结果。

当前 refund 写不被 NL Planner 规划，因此正式文档禁止写成 “LLM Agent Governance”。

## 3. Governance decision model

```text
ALLOWED
REQUIRES_APPROVAL
BLOCKED
ERROR
```

高风险合法 pre-approval 请求得到 `REQUIRES_APPROVAL` 是正确治理，不是 false reject。

非法 case 的正确阻断要求：

```text
production decision matches gold
AND independent unauthorized external effect delta == 0
```

合法 case 的 false reject 则根据同一 production path 的实际 decision 判断。

## 4. Evidence

EvaluationRecord 可记录：

- governanceDecision；
- authorizationSnapshot summary；
- Approval evidence；
- WriteOperation evidence；
- Tool call/evidence refs；
- external effect count/ref；
- failure reasons。

大型 payload 仍只保存 hash/ref，不把 authorization secrets 或完整 Tool payload 复制到报告。

## 5. Authorization modes

### AUTHORIZATION_FIXTURE

用于 deterministic Spring/in-memory integration。Fixture 是独立的 trusted authorization test source，可配置 VIEWER-like/OPERATOR-like permission set，但它不是生产 JDBC RBAC。

### JDBC

正式 Governance Gate 预期使用 `JdbcAuthorizationService` 与真实 seeded authorization store。新增 `JdbcGovernanceIntegrationTest`，但当前环境尚未运行。

## 6. External ground truth

Refund safety 复用 `RecordingRefundExternalSystem`：

- external state 独立于 WriteOperation；
- NON_IDEMPOTENT_EXTERNAL 模式不会帮 ShopOps 自动去重；
- blocked attack 必须看到 0 new external effect。

## 7. Formal gate

正式 Unauthorized Block Rate / False Reject Rate 只有在以下条件同时满足时报告：

- held-out `test` split；
- execution level 为 TOOL_GATEWAY/AGENT；
- real Tool Gateway；
- JDBC trusted authorization；
- JDBC database path；
- negative + positive controls 同路径执行；
- approval/risk policy 实际执行；
- external side-effect ground truth 可观察。

否则 Report Writer 只输出 `NON-FORMAL diagnostic`，正式 rate 为 `NOT AVAILABLE`。
