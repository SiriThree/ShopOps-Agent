# ShopOps Agent 自然语言批量评测记录

更新时间：2026-07-28

这份记录用于沉淀 ShopOps Agent 工作台的真实批量运行结果。它和离线异常识别评测不同，评测过程会真实请求后端自然语言任务入口：

```text
POST /api/agent/tasks/natural-language
```

系统会完成意图识别、任务创建、工具编排、报告生成、步骤查询和报告证据读取。

## 1. 评测范围

本轮评测覆盖真实公开数据映射后的 7 天业务日期：

```text
2018-08-01 至 2018-08-07
```

每个日期覆盖 4 类自然语言任务：

| 场景 | 目标 |
|---|---|
| daily_review | 店铺每日经营复盘 |
| comment_risk | 差评风险专项分析 |
| product_optimization | 低点击/待优化商品分析 |
| ad_anomaly | 投放异常专项检查 |

每轮包含 `7 * 4 = 28` 个 Agent 任务。本次执行 `10` 轮，共 `280` 个任务。

## 2. 数据来源

本轮批评测使用当前后端已配置的 5 个文件型真实数据连接器：

| 连接器 | 用途 |
|---|---|
| file.order-summary | Olist 订单汇总 |
| file.negative-comments | Olist 低分评价/差评风险 |
| file.product-candidates | Olist 商品优化候选 |
| file.ad-performance | Criteo 广告曝光、点击、转化、成本 |
| file.external-reports | Store Sales 外部事件背景与平台指标代理 |

## 3. 评测结果

产物位置：

```text
docs/evaluation/agent-natural-language-batch-summary.json
docs/evaluation/agent-natural-language-batch-summary.md
docs/evaluation/agent-natural-language-batch-details.csv
```

本次结果：

| 指标 | 数值 |
|---|---:|
| Agent 任务数 | 280 |
| 通过任务数 | 280 |
| 任务通过率 | 100% |
| 任务成功率 | 100% |
| 自然语言意图识别准确率 | 100% |
| 工具调用次数 | 1260 |
| 工具调用成功次数 | 1260 |
| 工具调用成功率 | 100% |
| 平均每任务工具调用数 | 4.5 |
| 平均端到端耗时 | 646.8 ms |
| P95 端到端耗时 | 863.5 ms |
| 平均任务内部耗时 | 514.3 ms |

## 4. 分场景结果

| 场景 | 任务数 | 通过数 | 成功率 | 平均工具数 | 平均耗时 |
|---|---:|---:|---:|---:|---:|
| daily_review | 70 | 70 | 100% | 6 | 783.3 ms |
| comment_risk | 70 | 70 | 100% | 4 | 606.6 ms |
| product_optimization | 70 | 70 | 100% | 4 | 599.1 ms |
| ad_anomaly | 70 | 70 | 100% | 4 | 598.1 ms |

## 5. 口径说明

- `任务成功率`：后端任务状态为 `SUCCESS` 的比例。
- `任务通过率`：任务完成、意图符合预期、生成报告且报告 evidence 中包含数据源证据。
- `工具调用成功率`：Agent 步骤中 `SUCCESS` 状态工具调用数 / 总工具调用数。
- `端到端耗时`：批评测脚本从创建自然语言任务到读取任务、步骤和报告详情的 HTTP 总耗时。
- `任务内部耗时`：后端任务 `startedAt` 到 `finishedAt` 的耗时。

## 6. 简历可引用口径

可以写：

```text
基于公开真实电商、广告和零售数据构建 Agent 批量评测流程，覆盖经营日报、差评分析、商品优化和投放异常 4 类自然语言任务；在 7 天业务窗口上连续执行 280 个 Agent 任务，完成 1260 次工具调用，任务成功率、意图识别准确率和工具调用成功率均达到 100%，平均端到端耗时 646.8 ms，P95 为 863.5 ms。
```

不要把这组指标表述为生产线上真实用户 SLA。它是本地公开数据和本地后端环境下的可复跑评测结果。

## 7. 复跑方式

先启动后端，并确认连接器状态正常，然后运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-agent-natural-language-batch.ps1 -Rounds 10 -DelayMs 0
```

如果只想快速冒烟验证，可以运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-agent-natural-language-batch.ps1 -Rounds 1 -DelayMs 0
```
