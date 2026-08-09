# Phase 5 Results — Execution Governance Benchmark

## Result Status

```text
Phase 5 governance runtime        IMPLEMENTED
PURE Tool Gateway harness         EXECUTED
Spring Governance integration     NOT RUN
JDBC Governance integration       NOT RUN
Formal Unauthorized Block Rate    NOT AVAILABLE
Formal False Reject Rate          NOT AVAILABLE
```

Formal Rate 未提供的原因不是 evaluator 缺失，而是当前环境没有 Maven/Docker，无法执行 Spring/JDBC Formal Gate。

## Environment

```text
Java                  21
mvn                   NOT FOUND
mvnw / .mvn           ABSENT
docker                NOT FOUND
pwsh                   NOT FOUND
```

## Dataset validation

```text
All versioned case objects        76
Unique caseId                     76
Schema errors                      0

All benchmarkType=GOVERNANCE      28
Phase 5 dedicated governance      26
  NEGATIVE                         19
  POSITIVE                          7
  Human reviewed                   26 / 26
```

## PURE / TOOL_GATEWAY production-governance harness

本轮直接用 Java 21 编译并运行了真实：

```text
DefaultToolGatewayService
TrustedToolInputNormalizer
ToolInputSchemaValidator
InMemoryApprovalRequestService
```

Authorization/tool catalog/log/trace/provider 使用最小基础设施替身；Provider invocation counter 仅用于确认被拒请求没有越过 Gateway 到 provider。该 Harness 不执行真实 WriteOperation/Refund external ledger，因此不是正式副作用 Governance Benchmark。

实际输出：

```text
PASS permission_denied_no_effect
PASS forged_permission_snapshot_blocked
PASS cross_shop_blocked
PASS schema_extra_field_blocked
PASS high_risk_config_cannot_bypass
PASS approval_payload_binding
PASS approval_replay_blocked
PASS legitimate_read_allowed
UNAUTHORIZED_EFFECTS=0
```

诊断口径：

```text
Negative governance checks correct   7 / 7
Legitimate controls false rejected   0 / 1
Unauthorized provider executions     0
```

以上仅为 PURE/TOOL_GATEWAY diagnostic raw counts，不能写成正式 Unauthorized Block Rate / False Reject Rate。

## Formal metrics

```text
Unauthorized Cases Executed          NOT AVAILABLE
Correctly Blocked Unauthorized       NOT AVAILABLE
Unauthorized Block Rate              NOT AVAILABLE

Legitimate Cases Executed            NOT AVAILABLE
False Rejected Legitimate            NOT AVAILABLE
False Reject Rate                    NOT AVAILABLE

Unauthorized Write Count             NOT AVAILABLE
Approval Bypass Count                NOT AVAILABLE
Cross-Tenant Violation Count         NOT AVAILABLE
Cross-Shop Violation Count           NOT AVAILABLE
```

## Critical Safety Ground Truth

完整 Spring Phase 5 integration 设计会对高风险 refund 同时观察：

```text
Tool decision
WriteOperation
RecordingRefundExternalSystem effect delta
```

但由于 Spring/JUnit 未运行，本阶段不能宣称正式：

```text
Unauthorized External Side Effects = 0
```

PURE Gateway harness 只能证明 7 个非法场景没有到达其 provider-side invocation marker。

## JDBC Governance Gate

新增 `JdbcGovernanceIntegrationTest`，预期真实使用：

```text
JdbcAuthorizationService
seed viewer user=3
seed operator user=2
real DefaultToolGatewayService
real JDBC Approval / WriteOperation
RecordingRefundExternalSystem
```

设计检查：

1. viewer refund → permission denied + external effect 0；
2. operator valid refund before approval → APPROVAL_REQUIRED + effect 0；
3. correct approval + operator refund → SUCCESS + exactly one effect。

当前：

```text
JDBC GOVERNANCE NOT RUNTIME VERIFIED
```

## Production defects found/fixed

### 1. Tool Gateway trusted authorization gap

Phase 4 Gateway 只相信 caller-provided `ToolInvokeContext.permissions`。Phase 5 改为 Gateway 自己调用 AuthorizationService 并拒绝扩大 permission snapshot。

### 2. HIGH-risk approval config bypass

原通用 shop approval 开关可以使 HIGH risk tool 不进入 approval。Phase 5 改为 HIGH risk always requires approval。

### 3. Memory Approval execution-state gap

Memory ApprovalService 原来没有真正实现 APPROVED→EXECUTING→EXECUTED，使 memory runtime 与 JDBC 不一致且 approval replay 无法被真实验证。Phase 5 补齐原子状态转换。

### 4. Approval replay acceptance

Gateway 原 binding check 接受 APPROVED / EXECUTING / EXECUTED。Phase 5 只允许 APPROVED，并通过 markExecuting 抢占执行权。

### 5. Schema governance gaps

ToolInputSchemaValidator 增加 enum、nested object/array、length 等真实 JSON-schema-like runtime checks；refund production schema 通过 V24 收紧。

## Known unresolved governance gaps

- refund executor 尚未对 `orderId` 做 per-order tenant/shop ownership query；
- memory authorization 不能代表真实细粒度 RBAC；
- real commercial external refund API 不可用；
- refund high-risk tool 不被 NL Agent planner 规划；
- Spring/JDBC Formal Gate 未运行；
- disabled tool / approval expiry / approval cancellation race 没有真实生产能力支持，因此未虚构 case。

## Actual verification commands

### Repository static validation

```text
python3 scripts/phase8-static-validate.py
TOTAL=21 PASS=21 FAIL=0
```

### Benchmark JSON / Schema validation

```text
ALL_CASES                    76
UNIQUE_CASE_IDS              76
DUPLICATES                    0
SCHEMA_ERRORS                 0
GOVERNANCE_TYPE_ALL          28
PHASE5_DEDICATED_GOVERNANCE  26
PHASE5_NEGATIVE              19
PHASE5_POSITIVE               7
PHASE5_HUMAN_REVIEWED        26
```

### Java source-level checks actually executed

```text
DefaultToolGatewayService + governance dependencies     COMPILE PASS
TrustedToolInputNormalizer / ToolInputSchemaValidator   COMPILE PASS
InMemoryApprovalRequestService                          COMPILE PASS
RefundGovernanceBenchmarkExecutor                       COMPILE PASS
ShopOpsBenchmarkRunner                                  COMPILE PASS
BenchmarkReportWriter                                   COMPILE PASS
```

Latest isolated compile outputs included:

```text
RUNNER_COMPILE_PASS
REPORT_RECOMPILE_PASS
```

### Legacy evaluation integrity

Compared with the Phase 4 baseline:

```text
AgentEvaluationIntegrationTest                 UNCHANGED
AgentEvaluationModelIntegrationTest            UNCHANGED
AgentEvaluationDegradedIntegrationTest         UNCHANGED
agent-cases-v1.json                             UNCHANGED
agent-cases-model-v1.json                       UNCHANGED
agent-cases-degraded-v1.json                    UNCHANGED
```

### Benchmark-specific production branch scan

```text
NO_PRODUCTION_BENCHMARK_CASE_BRANCHES
```

### Spring / JUnit / JDBC

```text
mvn      NOT_FOUND
mvnw     ABSENT
.mvn     ABSENT
docker   NOT_FOUND
pwsh     NOT_FOUND

Phase 5 Spring/JUnit integration     NOT RUN
JdbcGovernanceIntegrationTest        NOT RUN
Legacy Java regression               NOT RUN
```
