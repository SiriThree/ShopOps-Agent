# ShopOps 作品集中文演示讲稿

更新时间：2026-07-22

这份讲稿用于配合 `docs/ShopOps-portfolio-report.md` 和 Olist demo 摘要做项目展示。目标不是逐页介绍功能，而是把 ShopOps 讲成一个有工程闭环、有真实数据、有量化验收的 AgentOps 项目。

## 1. 开场定位

可以先用 30 秒把项目讲清楚：

> ShopOps 是一个面向电商运营场景的 AgentOps 管理后台。它不是单纯让大模型生成一段日报，而是把 Agent 的执行过程拆成任务、工具、报告、审批、审计、配置和评测几个可治理环节。目前我已经接入了 Olist 公开电商数据，让日报 Agent 基于真实订单和真实评价生成经营复盘，并且用评测用例量化验证 Agent 行为。

如果面试官追问“特色是什么”，可以补一句：

> 这个项目的特点是把 Agent 从 demo 做成了后台系统：能看执行链路，能管高风险动作，能查审计记录，能用店铺配置影响运行结果，也能用测试集评估是否稳定。

## 2. 必讲数字

展示时优先讲这些数字：

| 维度 | 数字 | 讲法 |
|---|---:|---|
|评测用例|14 个|覆盖日报、模型策略、审批、配置阈值、降级等场景|
|评测通过|14/14|当前基线全部通过|
|Agent 完成率|100%|成功、降级、审批等待都算可控完成|
|工具调用成功率|98.6%|工具链整体稳定|
|审批判断准确率|100%|高风险工具是否需要审批符合预期|
|配置生效准确率|100%|店铺配置会真实影响 Agent 执行|
|Olist 订单数|370|选取 2018-08-07 作为演示业务日|
|Olist GMV|62057.77|日报核心指标来自真实订单支付数据|
|退款代理率|7.63%|用 canceled / unavailable 订单金额作为售后风险代理值|
|风险评价数|51|来自 Olist 真实评价数据|
|商品候选数|10|基于低星评价聚合出的优化候选商品|

## 3. 演示前准备

先生成 Olist 连接器文件：

```powershell
python scripts/prepare_olist_demo.py
```

启动后端：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--shopops.connector.order-summary.file=docs/demo-data/olist/order-summary-olist.json --shopops.connector.negative-comments.file=docs/demo-data/olist/negative-comments-olist.json --shopops.connector.product-candidates.file=docs/demo-data/olist/product-candidates-olist.json"
```

跑一键验收并生成 demo 摘要：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-agentops-demo.ps1 -Port 8080 -Start 2018-08-07 -End 2018-08-07 -Scenario olist-agentops-demo -Dataset olist
```

刷新作品集战报：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-portfolio-report.ps1
```

## 4. 3 分钟快速演示

适合面试官时间有限时使用。

1. 打开 Dashboard

讲法：

> 这里是 AgentOps 后台总览。任务、报告、工具调用、失败事件和健康状态会聚合到 Dashboard，说明这个系统不是孤立跑一次 Agent，而是把 Agent 当成一个可运营的后台能力来管理。

2. 打开 Tasks，展示 `daily_review` 任务

讲法：

> 这条任务是日报 Agent。它会根据日期范围拆出工具调用链路，执行订单汇总、差评风险、商品候选、广告和外部报表查询，最后生成报告。每个任务都有 taskId、traceId、状态和执行结果。

重点数字：

- 业务日期：`2018-08-07`
- 最近一次 demo 示例：`taskId=10004`
- 状态：`SUCCESS`

3. 打开 Reports，展示 Olist 报告

讲法：

> 报告不是凭空生成的，它来自工具证据链。这里的 GMV、订单数、退款代理率、风险评价数和商品候选都来自 Olist 真实数据转换后的连接器文件。

重点数字：

- GMV：`62057.77`
- 订单数：`370`
- 退款代理率：`7.63%`
- 风险评价数：`51`
- 商品优化候选数：`10`

4. 打开 Approvals 或 Tools，展示高风险退款审批

讲法：

> 对高风险工具，系统不会直接执行。比如退款执行工具会先返回 `APPROVAL_REQUIRED`，生成审批单。审批时还需要输入二次确认语“确认通过”，通过后携带 approvalId 才能继续执行工具。

重点结果：

- 审批状态：`APPROVED`
- 审批后重试：`SUCCESS`
- 退款工具结果：`EXECUTED`

5. 打开 Audit Center

讲法：

> 审批创建、审批通过、工具执行都会进入审计时间线。这样 Agent 的关键动作可以被追踪、复盘和问责，这也是企业级 Agent 和普通 demo 的差别。

6. 最后展示战报

讲法：

> 我还做了一套评测基线，不只看功能能不能点通。当前 14 个用例全部通过，工具调用成功率 98.6%，审批判断和配置生效准确率都是 100%。这说明 Agent 的行为是被持续验证的。

## 5. 8 分钟完整版演示

适合项目展示或答辩。

1. Dashboard：先讲系统定位

要点：

- 这是电商运营 Agent 的管理后台。
- 关注点不是聊天，而是任务、工具、报告、审批、审计、配置。
- Dashboard 用来展示系统整体运行状态。

2. Organization / Shop Config：讲配置如何影响 Agent

要点：

- `refund_rate_warn_threshold` 控制退款率预警阈值。
- `negative_comment_warn_threshold` 控制差评预警阈值。
- `agent_tool_approval_enabled` 控制高风险工具是否需要审批。
- `agent_model_policy` 控制模型策略，并进入报告 evidence。

讲法：

> 店铺配置不是静态表单，它已经接入 Agent 运行期。日报生成时会读取阈值，高风险工具执行时会读取审批开关，报告 evidence 里也会沉淀本次配置快照。

3. Connectors：讲 Olist 真实数据接入

要点：

- `file.order-summary`：`UP`
- `file.negative-comments`：`UP`
- `file.product-candidates`：`UP`
- `file.ad-performance`：`NOT_CONFIGURED`
- `file.external-reports`：`NOT_CONFIGURED`

讲法：

> 当前 Olist 只覆盖订单、评价、商品三类真实数据。广告和平台外部指标 Olist 不提供，所以这两类暂时仍使用内置演示数据。我在战报里也明确写了这个边界。

4. Tasks：讲 Agent 执行链路

要点：

- 创建 `daily_review`。
- 日期选择 `2018-08-07`。
- 关注 `taskId`、`traceId`、状态、报告 ID。

讲法：

> Agent 任务不是黑盒执行。后台能看到任务从创建、执行到完成的状态变化，也能根据 traceId 串起报告、工具调用和审计事件。

5. Reports：讲真实数据报告

要点：

- GMV `62057.77`
- 订单数 `370`
- 退款代理金额 `4732.62`
- 退款代理率 `7.63%`
- 风险评价数 `51`
- 商品候选数 `10`
- 优先商品：`Furniture Bedroom / 4f18ca98`

讲法：

> 这里最重要的是 evidence。报告文本只是结果，evidence 才能证明结果来自哪些工具、哪些商品、哪些风险评价，以及本次使用了什么店铺配置。

6. Tools / Approvals：讲高风险动作控制

要点：

- 退款工具属于高风险工具。
- 默认需要审批。
- 未审批时返回 `APPROVAL_REQUIRED`。
- 审批需要二次确认语。
- 审批后工具执行成功。

讲法：

> 在企业场景里，Agent 不能直接做高风险动作。所以我把工具网关和审批中心连起来，形成“请求执行 -> 生成审批 -> 人工确认 -> 携带 approvalId 重试”的链路。

7. Audit Center：讲可追踪和治理

要点：

- 审批创建事件。
- 审批通过事件。
- 工具调用事件。
- 可按 source、resourceId、toolCode 筛选。

讲法：

> 审计中心的价值是把 Agent 的关键动作留下证据。出了问题可以回放，做验收时也可以证明系统确实做了权限控制和风险控制。

8. Portfolio Report：讲量化验收

要点：

- 打开 `docs/ShopOps-portfolio-report.md`。
- 讲 14 个评测用例。
- 讲通过率、工具调用成功率、审批准确率、配置准确率。
- 说明 Olist 边界。

讲法：

> 我没有只靠手工演示判断项目完成度，而是把 Agent 行为做成了评测基线。包括正常日报、模型策略、配置阈值、审批开关、高风险工具、模型失败降级这些场景，都有可重复执行的用例。

## 6. 面试追问回答

### 为什么说这是 AgentOps，而不是普通 AI 应用？

回答：

> 普通 AI 应用更关注生成结果，ShopOps 更关注执行过程可治理。它把 Agent 拆成任务、步骤、工具调用、报告、审批、审计、配置和评测。每个关键动作都有状态、有证据、有日志、有权限控制。

### Olist 数据接入解决了什么问题？

回答：

> 它让项目从纯 mock 走到半真实业务数据模式。订单指标、低星评价、商品优化候选来自真实公开电商数据，Agent 主链路不用改，只需要替换连接器输入。这说明系统的数据适配层是可替换的。

### 为什么退款是“代理值”？

回答：

> Olist 没有直接提供真实退款金额字段，所以我用 `canceled / unavailable` 订单的支付金额作为售后风险代理值。这个口径在文档里明确标注了，不把它包装成真实退款数据。

### 为什么广告和外部指标还没有真实接入？

回答：

> Olist 数据集不包含广告曝光、点击、花费，也不包含平台外部环境指标。为了保证口径诚实，这两类目前保留内置演示数据。后续可以接 Criteo 做广告，或者接其他时间序列数据做外部指标。

### Model Gateway 现在是真实模型吗？

回答：

> 当前这条 Olist demo 的报告生成模式是 `RULE`，也就是规则兜底路径。项目已经有 Model Gateway、Provider、Prompt Template、调用日志和降级链路，真实 API key 可以通过 Model Gateway 配置接入。我的展示重点是 AgentOps 工程闭环，真实模型调用是可插拔的一层。

### 这个项目最能体现工程能力的地方是什么？

回答：

> 我认为是三个点：第一，Agent 执行链路不是黑盒，任务、工具、报告、审批和审计都能追踪；第二，运行期配置能真实影响 Agent 行为，并且进入报告 evidence；第三，有评测基线和真实数据 demo，项目不是只靠页面演示，而是有可复现的验收数据。

## 7. 不建议主动展开的点

除非被问到，否则不要把时间放在这些点上：

- 前端还不是 React / Ant Design Pro 产品化形态。
- Olist 商品没有真实标题。
- 广告和外部报表还没有真实数据。
- 当前 Olist demo 使用规则报告生成。

这些不是不能讲，而是要放在“当前边界”里讲，不要让它们抢走主线。

## 8. 收尾话术

可以这样结束：

> 这个项目目前已经具备一个可展示的 AgentOps 闭环：真实数据输入、Agent 任务执行、工具证据链、配置生效、高风险审批、审计追踪和量化评测。后续如果继续做，我会优先补真实广告数据、真实模型调用验收，以及更产品化的前端体验。
