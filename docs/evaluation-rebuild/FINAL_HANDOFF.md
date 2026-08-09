# ShopOps Evaluation Rebuild — Final Handoff

## Evaluation 最终测什么？

只测四套相互独立的 ShopOpsBench v1 实验：

```text
Task / Effective
Idempotency / Safe
Recovery / Recoverable
Governance / Safe
```

不计算综合总分。

## 为什么不再使用 280/280？

旧自然语言批量数据来自 `4 prompt templates × 7 dates × 10 repetitions`，它代表 280 executions，不是 280 unique semantic tasks，也不观察真实业务 Outcome、External Side Effect、State Convergence 或合法/非法治理对照。

## Task Success 分母是什么？

只有正式 held-out Task run 中**有效执行**的 case 才进入分母；`NOT_EXECUTED` 和基础设施未启动不能直接拉低 Agent 成功率。每个 case 必须同时满足五个 binary conditions。

## Duplicate Side Effects 如何观察？

通过独立 `RecordingRefundExternalSystem` 记录 external attempts 与 `externalEffectId`，而不是通过 HTTP 调用数、WriteOperation 行数或本地状态推断。正式 Idempotency 仍要求 JDBC + Tool Gateway + NON_IDEMPOTENT_EXTERNAL。

## External Ground Truth 是什么？

当前退款 Ground Truth 是一个独立 in-process external test system。它不读取 WriteOperation/AgentTask，自行维护 accepted effects；它不是商业退款 API。

## State Convergence 如何定义？

```text
Converged = TerminalStateReached
            AND LocalStateConsistentWithExternalReality
```

`NEEDS_MANUAL_ACTION` 可以是安全终态，但不自动等于 automatic recovery。

## Governance 如何避免“全拒绝 = 100%”？

Governance dataset 同时包含 25 negative 与 8 positive controls，分别计算 Unauthorized Block Rate 与 False Reject Rate，并单独保留 Unauthorized Write / Approval Bypass / Cross-Tenant / Cross-Shop safety counts。

## 哪些指标来自真实 JDBC？

在当前交付环境中：**没有 Phase 6 formal metric 已经被 JDBC runtime 验证。** JDBC formal tests 已实现但未执行。

## 哪些来自 Test Double？

- Refund independent external ground truth：in-process test external system；
- Task formal test仍使用 MCP test adapter 作为外部 Commerce MCP infrastructure boundary；
- phase-specific PURE harnesses使用 deterministic infrastructure adapters。

## 哪些是真实模型？

当前 Phase 6 没有执行真实模型 formal run，因此 MODEL execution count = 0，MODEL_FALLBACK execution count = 0。生产代码具备 model planner path，但不能在本交付中给模型 benchmark 数字。

## 哪些是 Rule-based？

当前生产自然语言解释器主要仍是 `RuleBasedAgentTaskInterpreter`；专用 intents 大量使用 rule plan，daily_review 的 model path 仍存在 fixed-sequence validation/fallback。Phase 6 formal Task run没有执行，所以正式 RULE_BASED execution count 也是 0。

## 哪些可以写进简历？

当前可以写工程设计事实，不建议写 Phase 6 正式百分比。参见 `RESUME_METRIC_CANDIDATES.md`。

## 哪些不能写？

不能写 Formal Task Success、0 formal duplicate、formal convergence rate、formal governance rates、280 unique tasks、LLM refund governance、Rabbit refund redelivery、real commercial refund API 等未经验证的 claim。

## Phase 6 新增 production fix

退款外部写之前增加 JDBC business-object ownership enforcement：

```text
trusted tenantId + shopId + orderId
→ shop_order scoped lookup
→ remaining refundable amount check
→ only an owned, economically valid refund can reach approval/provider/external boundary
```

这修复 Phase 5 暴露的对象级 scope 设计缺口；由于 MySQL integration 未执行，当前状态为 IMPLEMENTED / FORMAL NOT VERIFIED。

## Formal runtime blockers

```text
Maven              NOT_FOUND
Maven Wrapper      ABSENT
Docker             NOT_FOUND
Spring/JUnit        NOT RUN
MySQL/JDBC          NOT RUN
RabbitMQ            NOT RUN
```

## 在正常开发机上的下一步

1. 安装/提供 JDK 17、Maven、Docker；
2. 启动 MySQL integration database；
3. `python3 scripts/verify-benchmark-manifest.py`；
4. 先运行完整 Maven regression；
5. 运行四套 `--formal` held-out benchmark；
6. 保留所有失败 case；
7. 建立第一版 formal baseline；
8. 再根据业务需求冻结 quality/reliability threshold；
9. 生成最终 release evidence pack。

## 最终交付定位

本轮交付的核心价值不是“所有指标 100%”，而是 ShopOps 已经拥有一套能够阻止 claim escalation 的评测工程：未跑到 Formal Gate 的事实会保持 `NOT AVAILABLE`。
