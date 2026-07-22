# ShopOps Agent 评测基线与作品集数据

更新时间：2026-07-21

这份文档用于固定当前 ShopOps Agent 模块的第一版量化评测基线，方便后续做三件事：

1. 面试或作品集展示时，直接引用稳定指标。
2. 后续引入真实业务数据、真实模型后，和当前基线做对比。
3. 每次功能迭代后，可以重复执行同一套评测，避免只讲功能不讲结果。

## 1. 当前评测目标

当前这版评测不是在测“模型回答是否好看”，而是在测 Agent 任务链路是否可控、可验证、可降级、可审计。

覆盖的核心能力有：

- 每日经营复盘任务是否能完成
- 店铺配置是否真的影响执行结果
- 高风险工具是否按配置决定是否审批
- 模型网关接入后，报告 evidence 是否保留模型调用元数据
- 工具失败时，任务是否进入可接受的降级完成状态

## 2. 当前评测范围

当前评测共分为 3 组套件、14 个案例。

### Core 套件

覆盖最基本的 Agent 任务主链路与审批链路。

- `daily-review-balanced-001`
- `daily-review-conservative-002`
- `tool-approval-on-001`
- `tool-approval-off-002`
- `daily-review-refund-alert-003`
- `daily-review-negative-alert-004`
- `tool-approval-off-large-005`

主要验证：

- 日报任务成功执行
- 店铺配置快照写入报告 evidence
- `agent_tool_approval_enabled` 控制高风险工具是否创建审批单

### Model 套件

覆盖模型网关参与的日报生成链路。

- `daily-review-model-balanced-001`
- `daily-review-model-aggressive-002`
- `daily-review-model-conservative-003`
- `daily-review-model-refund-alert-004`

主要验证：

- 任务成功执行
- 报告 evidence 中存在模型策略与模型调用相关字段
- `agent_model_policy` 配置对执行结果和证据链可见

### Degraded 套件

覆盖工具失败后的降级能力。

- `daily-review-degraded-balanced-001`
- `daily-review-degraded-conservative-002`
- `daily-review-degraded-aggressive-003`

主要验证：

- 指定工具失败后任务进入 `DEGRADED`
- 报告和配置快照仍然可追溯
- 降级路径不会把整条任务链直接打崩

## 3. 当前基线结果

本次基线结果来自自动生成文件：

- `shopops-admin/target/evaluation/agent-eval-portfolio-summary.md`
- `shopops-admin/target/evaluation/agent-eval-portfolio-summary.json`

2026-07-21 的结果如下：

| 指标 | 数值 |
|---|---:|
| 总案例数 | 14 |
| 通过案例数 | 14 |
| 完成率 | 100% |
| 成功率 | 71.4% |
| 降级完成率 | 21.4% |
| 平均任务耗时 | 34.6 ms |
| 工具调用成功率 | 98.6% |
| 审批决策准确率 | 100% |
| 配置生效率 | 100% |

状态分布：

- `SUCCESS`: 10
- `DEGRADED`: 3
- `APPROVAL_REQUIRED`: 1

套件分布：

| 套件 | 案例数 | 通过 | SUCCESS | DEGRADED | APPROVAL_REQUIRED |
|---|---:|---:|---:|---:|---:|
| Core | 7 | 7 | 6 | 0 | 1 |
| Model | 4 | 4 | 4 | 0 | 0 |
| Degraded | 3 | 3 | 0 | 3 | 0 |

## 4. 指标口径说明

为了后续和真实业务接入阶段做对比，这里把口径固定下来。

### 完成率

定义：状态属于 `SUCCESS`、`DEGRADED`、`APPROVAL_REQUIRED` 的案例占比。

说明：

- `SUCCESS` 表示任务或工具调用完整成功。
- `DEGRADED` 表示链路部分失败，但系统按预期走了降级路径。
- `APPROVAL_REQUIRED` 表示高风险动作被正确拦截，属于治理成功，不算失败。

### 成功率

定义：状态为 `SUCCESS` 的案例占比。

说明：

- 这是更严格的成功口径。
- 随着未来引入真实外部依赖，这个指标通常会低于完成率。

### 降级完成率

定义：状态为 `DEGRADED` 的案例占比。

说明：

- 这个值高并不一定代表坏事。
- 在故障演练套件里，它代表“系统能带伤完成”，体现的是鲁棒性。

### 工具调用成功率

定义：所有案例中成功工具调用次数 / 工具调用总次数。

说明：

- 当前统计口径把审批拦截场景中的高风险工具视作一次调用，但不算成功执行。
- 这个指标适合描述 Agent 工具链稳定性。

### 审批决策准确率

定义：需要验证审批预期的案例中，实际是否创建审批单与预期是否一致的比例。

说明：

- 当前基线是 100%，说明 `agent_tool_approval_enabled` 已真实影响执行逻辑。

### 配置生效率

定义：需要验证配置影响的案例中，报告 evidence、日志结果或执行状态与预期配置一致的比例。

说明：

- 当前基线是 100%，说明店铺配置已经不是“静态数据”，而是运行时生效配置。

## 5. 如何复跑评测

执行脚本：

```powershell
./scripts/run-agent-evaluation.ps1
```

脚本会自动完成两步：

1. 执行三组评测测试类
2. 聚合生成作品集级汇总文件

底层测试命令等价于：

```powershell
mvn -pl shopops-admin "-Dtest=AgentEvaluationIntegrationTest,AgentEvaluationModelIntegrationTest,AgentEvaluationDegradedIntegrationTest" test
```

生成产物位置：

```text
shopops-admin/target/evaluation/agent-eval-summary.json
shopops-admin/target/evaluation/agent-eval-model-summary.json
shopops-admin/target/evaluation/agent-eval-degraded-summary.json
shopops-admin/target/evaluation/agent-eval-portfolio-summary.json
shopops-admin/target/evaluation/agent-eval-portfolio-summary.md
```

## 6. 作品集建议写法

如果你想把这部分直接写进简历、项目说明或演示材料，建议重点突出“治理能力”和“量化验证”。

可以这样写：

```text
为电商运营 Agent 后台设计并落地了首版自动化评测基线，覆盖任务主链路、模型接入、审批治理、配置生效和故障降级 3 类套件共 14 个案例；当前基线完成率 100%，工具调用成功率 98.6%，审批决策准确率 100%，配置生效率 100%，为后续真实业务数据与真实模型接入提供了可对比的量化基准。
```

如果要更强调 Agent 特色，可以这样讲：

```text
项目重点不只是“能生成报告”，而是把 Agent 执行拆成可验证的任务、步骤、工具调用、审批和降级链路，并用自动化评测固定效果数据，证明配置、审批和模型策略会真实影响执行结果。
```

## 7. 当前基线的局限

这份基线已经足够支撑第一轮作品集展示，但它还不是最终形态。

当前局限主要有：

- 仍以演示数据和集成测试驱动为主，尚未接入真实业务订单/评价/广告数据
- 案例量已经扩到 14 个，但距离真实生产验收所需样本量仍然偏小
- 还没有成本、token、响应时延分位数等模型侧指标
- 还没有把结果展示到管理后台页面
- 还没有形成按周或按版本沉淀的趋势图

## 8. 下一阶段建议

为了尽快做出“完整展示”，下一阶段建议按这个顺序推进：

1. 扩充第二批评测案例，把真实业务波动更大的场景补上
2. 接入一类真实业务数据源，优先订单或评价
3. 给评测结果补一个后台可视化页面或静态展示页
4. 增加模型成本、延迟、降级占比等更像生产系统的指标

最优先的下一步建议是：

```text
下一步把评测从“基线可用”推进到“展示更强”，优先补真实数据接入后的案例，并把当前 14 个案例沉淀成可持续复跑的版本基线。
```

现在这份基线已经足够支撑作品集展示；接下来最该补的是“真实业务数据后的对比结果”，而不是继续单纯堆案例数量。
