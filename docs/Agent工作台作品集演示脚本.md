# Agent 工作台作品集演示脚本

这份脚本用于面试或项目展示时快速讲清楚 ShopOps 的 Agent 特色：运营人员用自然语言发起任务，系统自动路由意图、编排工具、读取业务数据、生成量化报告，并把工具调用和结果沉淀到审计链路。

## 1. 展示目标

核心一句话：

> ShopOps 不是单纯的报表页面，而是一个电商运营 Agent 工作台。它把自然语言任务、工具编排、权限审批、运行配置、量化报告和审计追踪串成一条可观测链路。

建议展示重点：

- 自然语言入口：运营人员不用理解内部任务类型。
- Agent 路由：识别经营日报、差评专项、商品优化、投放异常。
- 工具编排：订单、评价、商品、投放、外部指标、报告生成分步骤执行。
- 量化结果：GMV、退款率、差评数、商品候选、广告 ROI 等指标直接展示。
- 审计闭环：任务、工具日志、报告、审计中心互相可跳转。

## 2. 演示准备

启动后端：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

打开工作台：

```text
http://localhost:8080/admin/workbench.html
```

如果要展示 Olist 真实数据版本，先准备数据并带上文件 Connector 参数：

```powershell
python scripts/prepare_olist_demo.py

mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080 --shopops.connector.order-summary.file=docs/demo-data/olist/order-summary-olist.json --shopops.connector.negative-comments.file=docs/demo-data/olist/negative-comments-olist.json --shopops.connector.product-candidates.file=docs/demo-data/olist/product-candidates-olist.json"
```

Olist 推荐日期：

```text
2018-08-07
```

## 3. 标准演示流程

1. 打开 `Agent 工作台`，先说明这是主入口。
2. 点击 `经营日报` 演示任务。
3. 观察 `Agent 理解结果`：看意图、关注点、数据源、推荐动作。
4. 观察 `本次执行`：看任务 ID、状态、意图、置信度。
5. 观察 `演示链路`：确认自然语言、工具编排、报告生成、审计追踪逐步点亮。
6. 观察 `工具编排`：说明 Agent 不是一次性生成文本，而是按工具步骤获取证据。
7. 观察 `量化结果`：重点讲 GMV、退款率、差评数、商品候选和广告 ROI。
8. 观察 `关键结论`：说明报告 evidence 里沉淀了工具链、配置快照和生成路径。
9. 打开 `报告`、`工具日志`、`审计` 链接，展示链路可追踪。

## 4. 四个演示任务

| 按钮 | 目标 | 预期意图 | 重点观察 |
|---|---|---|---|
| 经营日报 | 汇总店铺整体经营表现 | daily_review | GMV、退款率、差评、商品、投放、外部指标 |
| 差评专项 | 分析低星评价和风险商品 | comment_risk | 风险评价样本、差评数、受影响商品 |
| 商品优化 | 找出低点击或待优化商品 | product_optimization | 商品候选、评分、库存、优化原因 |
| 投放异常 | 检查高消耗低转化计划 | ad_anomaly | 广告消耗、ROI、投放计划表现 |

## 5. 可量化讲法

可以用这组口径收束项目价值：

- 任务不是黑盒：每次执行都有任务 ID、步骤、工具日志和审计记录。
- 报告不是空泛文本：报告 evidence 包含工具链、业务指标、配置快照和模型/规则生成路径。
- 数据可替换：内置 demo 数据可以替换为 Olist 文件 Connector。
- 行为可验证：当前测试基线覆盖自然语言入口、Agent 编排、审批、审计、模型网关和配置生效。

如使用当前 portfolio report，可引用这些量化结果：

| 指标 | 当前结果 |
|---|---:|
| Agent evaluation cases | 14 |
| Evaluation passed cases | 14 |
| Tool invocation success rate | 98.6% |
| Approval decision accuracy | 100% |
| Config effect accuracy | 100% |
| Olist GMV | 62057.77 |
| Olist order count | 370 |
| Olist risk comment count | 51 |

## 6. 面试讲解词

开场：

> 我把这个项目定位成电商运营 AgentOps 工作台，不只是做一个 AI 聊天框。运营同学可以用自然语言发起任务，后端会识别意图，编排订单、评价、商品、投放等工具，生成结构化报告，并把每一步调用、审批和审计都记录下来。

展示 Agent 工作台：

> 这里是主入口。我点一个经营日报，系统会把自然语言路由成 daily_review 任务，并展示它理解到的关注点、数据源和动作建议。

展示工具编排：

> 这里可以看到 Agent 的执行不是一次 prompt 结束，而是拆成订单汇总、差评查询、商品候选、投放表现、外部指标、报告生成这些步骤。每一步都有工具码和状态。

展示量化结果：

> 这些指标来自工具输出，不是前端写死的文本。比如 GMV、退款率、差评数、待优化商品和广告 ROI，会随着 Connector 数据变化。

展示审计闭环：

> 最后任务、报告、工具日志和审计中心可以互相跳转。这样如果运营建议触发了高风险工具，比如退款执行，也能通过审批和审计链路追踪。

收尾：

> 这个项目的重点是把 Agent 从“能回答”推进到“能执行、可追踪、可配置、可评测”。后续接真实平台 API 时，只需要替换 Connector，任务编排和审计体系可以复用。

## 7. 验证命令

工作台与自然语言入口：

```powershell
mvn -pl shopops-admin "-Dtest=AdminWorkbenchStaticPageIntegrationTest,AgentNaturalLanguageTaskIntegrationTest" test
```

全量测试：

```powershell
mvn -pl shopops-admin -am test
```

刷新评测报告：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-agent-evaluation.ps1
powershell -ExecutionPolicy Bypass -File scripts/generate-portfolio-report.ps1
```
