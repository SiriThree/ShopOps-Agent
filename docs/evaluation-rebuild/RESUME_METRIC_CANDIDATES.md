# Resume Metric Candidates

## Current decision

There are **no Phase 6 formal percentage/count metrics approved for resume use yet**.

Reason: the held-out Spring + JDBC formal runtime did not execute in the current environment. PURE/static diagnostics must not be promoted into resume claims.

## Eligible only after a future formal run

A future candidate must include all of:

```text
statement
benchmark
runId
benchmarkVersion
datasetVersion
executionLevel
numerator
denominator
environment
limitations
```

### Task candidate template

Only after a valid formal Task run:

```text
在 N 个 held-out 电商运营任务中，n / N 达到 End-to-End Agent Task Success。
```

### Idempotency candidate template

Only after real Tool Gateway + JDBC WriteOperation + independent non-idempotent external ground truth:

```text
在 N 个逻辑写请求、X 次重复请求/重试执行中，产生 N 个有效业务副作用，重复副作用 D 次。
```

Do not call repeated Tool-Gateway requests “MQ redelivery” unless an actual Rabbit consumer was involved.

### Recovery candidate template

Only after eligible JDBC recovery execution:

```text
在 N 个故障注入场景中，n / N 在有限恢复预算内达到 State Convergence。
```

### Governance candidate template

Only after eligible JDBC authorization + negative/positive held-out execution:

```text
非法请求正确阻断 n / N；合法请求误拒 m / M；未授权外部写、审批绕过、跨租户违规分别为 ...
```

## Non-metric implementation wording that is currently defensible

Without inventing a percentage, the project can be described as implementing:

> 设计 ShopOpsBench，将 Agent 评测拆分为任务完成、写操作幂等、故障状态收敛和执行治理四套独立实验，并建立 held-out 数据集冻结、可审计 Evidence Record、独立外部副作用 Ground Truth、Formal Eligibility 与安全 Release Gate。

This is an implementation statement, not a benchmark score.
