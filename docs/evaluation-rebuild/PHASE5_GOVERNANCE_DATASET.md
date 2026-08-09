# Phase 5 Governance Dataset

## Version

```text
datasetVersion = 1.4.0-phase5-governance
goldVersion    = shopopsbench-gold-v1.4
```

专用路径：

```text
benchmark/v1/governance/
├── dev/cases.json
├── validation/cases.json
└── test/cases.json
```

## Counts

```text
Phase 5 dedicated cases  26
dev                       9
validation               12
test                      5

NEGATIVE                 19
POSITIVE                  7
Human reviewed           26 / 26
```

全仓还有 2 条 Phase 0 的 GOV contract example，因此整个版本化资源树统计 `benchmarkType=GOVERNANCE` 时为 28；Phase 5 正式专用数据集仍是 26 条，两个口径不得混淆。

## Attack distribution

| Attack / control family | Cases |
|---|---:|
| SCHEMA | 5 |
| PERMISSION_FORGERY | 2 |
| APPROVAL_TARGET_MISMATCH | 2 |
| APPROVAL_REPLAY | 2 |
| LEGITIMATE_APPROVED_WRITE | 2 |
| LEGITIMATE_MCP_READ | 2 |
| CROSS_SHOP | 1 |
| CROSS_TENANT | 1 |
| IDENTITY_FORGERY | 1 |
| PERMISSION | 1 |
| APPROVAL_MISSING | 1 |
| APPROVAL_PAYLOAD_MUTATION | 1 |
| UNKNOWN_TOOL | 1 |
| LEGITIMATE_READ | 1 |
| APPROVAL_REJECTED | 1 |
| LEGITIMATE_HIGH_RISK | 1 |
| LEGITIMATE_IDEMPOTENT_REPLAY | 1 |

## Read / write

```text
WRITE   22
READ     4
```

## Risk

```text
HIGH     22
LOW       3
UNKNOWN   1   # deliberately unknown-tool case
```

## Expected decisions

```text
BLOCKED             18
ALLOWED              6
REQUIRES_APPROVAL    2
```

`REQUIRES_APPROVAL` 对合法 high-risk pre-approval control 和未审批攻击边界都是可审计 decision；Evaluator 使用 case class + expectedReason 区分正确治理与 false reject。

## Authorization mode

当前 26 条 Phase 5 deterministic cases：

```text
AUTHORIZATION_FIXTURE = 26
```

因此这些 case 即使 Spring memory runtime 全部跑通，也不能升级成正式 JDBC RBAC 指标。

另有 gated `JdbcGovernanceIntegrationTest` 使用真实 `JdbcAuthorizationService` 和 seed authorization data，但当前环境尚未运行。

## Held-out policy

`test` split 包含：

- approval target mismatch；
- approval replay；
- legitimate approved refund；
- legitimate idempotent replay with new approval；
- legitimate MCP read。

统一脚本继续要求显式 `-FormalTest` 才能运行 held-out test，不用于日常 evaluator development。

## Known coverage limits

1. 没有 disabled-tool case，因为当前 registry 虽支持 enabled，但本轮没有为了 Benchmark 新造生产 feature state；
2. 没有 “refund orderId belongs to other shop” 的正式 business-scope outcome，因为当前 refund executor 尚缺 per-order ownership lookup；
3. 没有 approval expiry attack，因为当前 Approval Model 没有真实 expiry contract；
4. 没有 approval cancel race，因为当前模型没有该生产状态；
5. 不通过 LLM 生成攻击 payload；攻击从 TOOL_GATEWAY execution level 进入，且明确记录该层级。
