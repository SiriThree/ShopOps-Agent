# Phase 5 Handoff

## 1. Phase status

```text
Governance contract/runtime code       IMPLEMENTED
Dedicated governance dataset           IMPLEMENTED
Unified runner/report integration       IMPLEMENTED
PURE production Tool Gateway checks     EXECUTED
Spring integration                      NOT RUN
JDBC authorization/write governance     NOT RUN
Formal governance rates                 NOT AVAILABLE
```

## 2. Real governance entry

Phase 5 主线从：

```text
TOOL_GATEWAY
→ DefaultToolGatewayService
→ AuthorizationService
→ TrustedToolInputNormalizer
→ ToolInputSchemaValidator
→ permission/risk/approval
→ ToolProvider
→ HighRiskRefundExecuteExecutor
→ WriteOperation
→ External Ground Truth
```

进入。

refund write 当前不属于 Natural Language Agent planner 可达工具，因此不要将 Phase 5 TOOL_GATEWAY 结果描述为 LLM Agent Governance。

## 3. Trusted identity facts

最终 trusted identity 是：

```text
tenantId / shopId / userId
→ AuthorizationService.resolve(...)
→ AuthorizationSnapshot
```

Gateway 自身再次解析 trusted authorization。Caller/Agent 不能通过 ToolInvokeContext permission set 或 arguments.permission/roles 提升权限。

## 4. Authorization modes

- `AUTHORIZATION_FIXTURE`：Phase 5 deterministic/in-memory integration；非正式 RBAC。
- `JDBC`：真实 `JdbcAuthorizationService` + seeded user/member store；正式 Gate 需要它真实运行。
- `InMemoryAuthorizationService`：admin-like fixture，只可做传播 smoke。

## 5. Approval facts

Approval 当前绑定：

```text
tenant/shop
requester
toolCode
canonical input hash
businessObjectId
risk level
source task/step/tool call
```

执行只允许：

```text
APPROVED
→ markExecuting atomic/CAS
→ provider
→ EXECUTED / execution failed
```

HIGH risk 不再允许通过 generic shop approval flag 绕过。

## 6. Dataset ready for Phase 6

Phase 5 专用：

```text
26 governance cases
19 NEGATIVE
7 POSITIVE
9 dev / 12 validation / 5 test
```

全仓另有 2 条 Phase 0 governance contract examples；统一统计时必须区分“Phase 5 dedicated”与“all benchmarkType=GOVERNANCE”。

## 7. Formal metrics still unavailable

在没有真实 Spring + JDBC Governance run 前：

```text
Unauthorized Block Rate  NOT AVAILABLE
False Reject Rate         NOT AVAILABLE
Unauthorized Writes      NOT AVAILABLE
Approval Bypass           NOT AVAILABLE
Cross-Tenant Violations   NOT AVAILABLE
Cross-Shop Violations     NOT AVAILABLE
```

不得用 PURE harness 的 7/7、0/1 替代正式指标。

## 8. Production fixes that Phase 6 must preserve

1. DefaultToolGatewayService trusted authorization re-resolution；
2. caller permission snapshot cannot exceed trusted authorization；
3. trusted tenant/shop/user argument conflict rejection；
4. HIGH-risk approval cannot be config-bypassed；
5. Approval single-consumption state machine；
6. only APPROVED approval can enter provider execution；
7. stricter refund schema + ToolInputSchemaValidator。

Phase 6 release gate 必须把这些作为回归门禁，而不能只看最终百分比。

## 9. Known limitations Phase 6 must expose

- NL Agent high-risk write unreachable；
- refund order business-object ownership validation incomplete；
- in-process external test system != commercial API；
- JDBC formal runtime not verified；
- Rabbit refund consumer does not exist；
- legacy 14-case evaluation remains fixed-workflow regression, not ShopOpsBench task/governance score。

## 10. Phase 6 direct reuse

Phase 6 可直接复用：

```text
Task Benchmark (Phase 2)
Idempotency Benchmark (Phase 3)
Recovery Benchmark (Phase 4)
Governance Benchmark (Phase 5)

ShopOpsBenchmarkRunner
EvaluationRun
EvaluationRecord
BenchmarkReportWriter
Dataset Version Contract
FailureReason taxonomy
RecordingRefundExternalSystem
ReliabilityFaultController
Authorization / Approval / Tool Gateway evidence
```

Phase 6 的重点应是：

```text
Unified ShopOpsBench
+ Release Gate
+ Legacy Evaluation Migration
+ Final Benchmark Report
```

仍然禁止把 Task / Idempotency / Recovery / Governance 平均成一个 Overall Score。
