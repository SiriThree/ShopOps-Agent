# ShopOps Agent

ShopOps Agent 是一个面向电商运营场景的 AgentOps 管理平台。项目目标不是做一个简单的 AI 聊天框，而是把运营人员的自然语言任务，拆解成可编排、可审计、可审批、可配置、可评测的 Agent 执行链路。

当前最适合作品集展示的主线是：

> 运营人员在 Agent 工作台用自然语言发起日常任务，系统自动识别意图，调用订单、评价、商品、投放等工具，生成结构化运营日报、异常告警和改进建议，并沉淀任务、工具、报告、审批、审计和量化评测数据。

## 项目亮点

- 自然语言 Agent 工作台：支持“生成今天店铺运营日报”“分析最近差评原因”“找出低点击商品并给优化建议”等任务输入。
- MCP 风格工具层：统一注册和调用订单、评价、商品、广告、外部报表等工具，调用过程进入工具日志和审计链路。
- Agent 任务编排：支持任务创建、意图路由、步骤执行、异步调度、失败重试、降级处理和任务追踪。
- 运营报告生成：输出 Markdown 运营日报、量化指标、证据链、配置快照和动作建议。
- 审批与风控：高风险工具支持人工审批、确认语校验、审批撤回、批量处理和过期处理。
- 店铺运行配置：退款率阈值、差评阈值、审批开关、模型策略可按店铺配置，并在 Agent 执行中真实生效。
- 全链路审计：任务、工具、报告、审批、模型调用、配置变更都可追踪，便于排障和验收展示。
- 真实数据演示：已接入 Olist 公开电商数据作为日报主链路，并用 Criteo、UCI Online Retail、Store Sales 构建公开多源真实数据基线。
- React 管理前端：管理后台已迁移到 React + TypeScript + Axios + ECharts + Ant Design。
- 量化评测基线：提供 Agent 评测脚本、作品集报告和可复现的验收数据。

## 当前完成度

| 模块 | 状态 | 说明 |
|---|---|---|
| Agent 工作台 | 已完成 v1 | 自然语言输入、快捷任务、Olist 演示、执行步骤、量化结果、报告入口 |
| Agent 任务流 | 已完成 | 创建、运行、重试、步骤、事件、任务详情、异步/同步执行 |
| MCP 工具中心 | 已完成 | 工具注册、工具网关、调用日志、失败统计、工具审批状态 |
| 审批中心 | 已完成 | 创建、通过、拒绝、撤回、批量操作、过期处理、确认语 |
| 审计中心 | 已完成 | 总览、风险事件、时间线、详情、导出、跨页面跳转 |
| 报告中心 | 已完成 | 报告列表、详情、Markdown 查看、报告证据链 |
| 用户与租户 | 已完成 | 用户、租户、店铺、成员、角色权限、密码重置 |
| 店铺配置 | 已完成 | 配置维护、运行期读取、阈值和审批开关生效 |
| Model Gateway | 已完成基础版 | Provider、Prompt 模板、调用日志、OpenAI-compatible 适配、规则 fallback |
| React 前端 | 已完成主页面迁移 | 工作台、Dashboard、任务、报告、审计、工具、审批、模型、组织等页面 |
| Olist 数据 | 已完成演示版 | 订单、评价、商品候选真实数据接入 |
| 公开多源真实数据基线 | 已完成 v1 | Criteo 广告、UCI 退款/取消代理、Store Sales 外部事件已纳入评测基线 |

## 快速开始

环境要求：

- JDK 17
- Maven 3.9+
- Node.js 18+
- Python 3.10+
- 可选：Docker、MySQL、Redis、RabbitMQ

准备 Olist 演示数据：

```powershell
python scripts/prepare_olist_demo.py
```

启动后端：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

打开 Agent 工作台：

```text
http://localhost:8080/admin/workbench.html
```

推荐演示日期：

```text
2018-08-07
```

工作台里可以点击 “Olist 演示数据”，再启动 Agent 任务。

## 推荐演示流程

1. 打开 `/admin/workbench.html`，说明这是运营人员的主入口。
2. 输入或选择快捷任务，例如“基于 Olist 真实订单和评价数据，生成 2018-08-07 店铺运营日报”。
3. 启动 Agent，观察意图识别、任务创建、步骤执行和工具调用过程。
4. 查看量化指标：GMV、退款代理率、风险评价数、商品候选数。
5. 打开最终报告，展示 Markdown 经营日报、证据链和店铺配置快照。
6. 跳转任务详情，展示步骤、状态、重试和追踪能力。
7. 跳转工具日志，展示 MCP 工具调用记录。
8. 跳转审计中心，展示任务、报告、工具、审批的关联链路。
9. 展示审批中心，说明高风险动作需要人工确认。
10. 最后展示评测报告，强调项目不是只做页面，而是有量化验收。

## Olist 数据演示

项目默认读取：

```text
docs/demo-data/olist/order-summary-olist.json
docs/demo-data/olist/negative-comments-olist.json
docs/demo-data/olist/product-candidates-olist.json
```

这些文件由脚本从 Brazilian E-Commerce Public Dataset by Olist 生成：

```powershell
python scripts/prepare_olist_demo.py
```

当前 Olist 演示摘要：

| 指标 | 结果 |
|---|---:|
| 业务日期 | 2018-08-07 |
| GMV | 62057.77 |
| 订单数 | 370 |
| 售后/退款代理金额 | 4732.62 |
| 售后/退款代理率 | 7.63% |
| 风险评价数 | 51 |
| 商品候选数 | 10 |

说明：

- Olist 不包含真实退款金额，项目使用 `canceled / unavailable` 订单支付金额作为售后风险代理值。
- Olist 不包含真实广告投放数据，广告表现已在公开多源基线中使用 Criteo Attribution 数据补齐；当前 Olist 在线演示连接器仍可回退到内置演示数据。
- Olist 不包含平台外部环境指标，外部事件已在公开多源基线中使用 Store Sales `holidays_events.csv` 补齐；当前 Olist 在线演示连接器仍可回退到内置演示数据。
- Olist 不提供商品标题，当前使用英文类目和 productId 前缀生成展示名称。

## Agent 主链路

```text
自然语言任务
  -> 意图识别与路由
  -> Agent task
  -> Planner 生成执行步骤
  -> Tool Gateway 统一调用工具
  -> order.query_summary
  -> comment.query_negative
  -> product.query_candidates
  -> ad.query_performance
  -> report.query_external_metrics
  -> report.generate_daily_review
  -> 运营报告
  -> 工具日志 / 审批中心 / 审计时间线
```

## 架构概览

```text
React Admin Console
  ├── Agent Workbench
  ├── Dashboard
  ├── Tasks / Reports
  ├── Tool Logs / Approval Center
  ├── Audit Center
  ├── Model Gateway
  └── Organization / Shop Config

Spring Boot Backend
  ├── Auth / Tenant / Shop
  ├── Agent Task Queue
  ├── Planner / Executor
  ├── MCP-style Tool Registry
  ├── Tool Gateway
  ├── Approval Service
  ├── Report Service
  ├── Audit Service
  ├── Model Gateway
  └── Connector Layer

Persistence and Integrations
  ├── Memory mode for local demo and tests
  ├── JDBC / MySQL mode for dev deployment
  ├── Redis and RabbitMQ optional infrastructure
  ├── Olist file connectors
  └── OpenAI-compatible model provider
```

## 技术栈

后端：

- Java 17
- Spring Boot 3.3
- Maven 多模块
- Spring MVC
- MyBatis
- Flyway
- MySQL
- Redis
- RabbitMQ
- JUnit 5
- Spring Boot Test

前端：

- React 19
- TypeScript
- Vite
- Axios
- ECharts
- Ant Design
- Ant Design Icons

Agent 与平台能力：

- MCP-style Tool Registry
- Agent Task Queue
- Rule Planner
- Sequential Executor
- Tool Gateway
- Approval Center
- Audit Timeline
- Report Center
- Model Gateway
- OpenAI-compatible Provider
- Shop Runtime Configuration

## 主要页面

| 页面 | 地址 | 用途 |
|---|---|---|
| Agent 工作台 | `/admin/workbench.html` | 自然语言发起任务，查看执行过程和最终报告 |
| Dashboard | `/admin/dashboard.html` | 总览任务、报告、工具、审计指标 |
| 任务中心 | `/admin/tasks.html` | 查看任务列表、详情、步骤、重试 |
| 报告中心 | `/admin/reports.html` | 查看运营报告、Markdown 内容、证据链 |
| 审计中心 | `/admin/audit.html` | 查看风险事件、时间线、关联详情 |
| 工具中心 | `/admin/tools.html` | 查看工具注册、调用日志、失败统计 |
| 审批中心 | `/admin/approvals.html` | 处理高风险工具审批 |
| 模型网关 | `/admin/prompts.html` | 管理 Prompt、模型调用和 Provider |
| 组织管理 | `/admin/users.html` | 管理用户、租户、店铺、成员、店铺配置 |

## 常用命令

后端全量测试：

```powershell
mvn -pl shopops-admin -am test
```

前端构建：

```powershell
cd shopops-admin-ui
npm run build
```

Agent 工作台相关测试：

```powershell
mvn -pl shopops-admin "-Dtest=AdminWorkbenchStaticPageIntegrationTest,AgentNaturalLanguageTaskIntegrationTest" test
```

刷新 Agent 评测基线：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-agent-evaluation.ps1
```

生成作品集量化报告：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-portfolio-report.ps1
```

验证 Olist 演示链路：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-agentops-demo.ps1 -Port 8080 -Start 2018-08-07 -End 2018-08-07 -Scenario olist-agentops-demo -Dataset olist
```

启动 MySQL / Redis / RabbitMQ 开发环境：

```powershell
docker compose -f deploy/docker-compose.dev.yml up -d
```

JDBC dev profile 启动：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## 量化验收

当前作品集基线：

| 维度 | 结果 |
|---|---:|
| Agent evaluation cases | 14 |
| Passed cases | 14 |
| Completion rate | 100% |
| Tool invocation success rate | 98.6% |
| Approval decision accuracy | 100% |
| Config effect accuracy | 100% |
| Public real-data business samples | 760 |
| Public real-data derived MCP tool calls | 2670 |
| Public real-data high-risk approval-routed calls | 450 |
| Olist real orders | 99441 |
| Olist real reviews | 99224 |
| Olist real products | 32951 |
| Criteo real ad impressions | 16468027 |
| Criteo real ad clicks | 5947563 |
| Criteo real ad conversions | 806196 |
| UCI Online Retail lines | 541909 |
| UCI cancellation/refund proxy lines | 10624 |
| Store Sales holiday events | 350 |
| 最近全量测试 | 86 tests, 0 failures, 8 skipped |

评测覆盖：

- 日常经营日报任务
- 差评风险识别
- 商品优化候选识别
- 工具调用成功率
- 高风险工具审批
- 关闭审批后的直接执行
- 店铺阈值配置生效
- 模型策略进入报告 evidence
- 模型失败后的降级完成

## Model Gateway

默认报告生成可以走规则 fallback，因此本地无需真实 API Key 也能完成演示。若要启用真实模型调用，可配置 OpenAI-compatible provider：

```powershell
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_ENABLED="true"
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_BASE_URL="https://your-provider.example/v1"
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_API_KEY="your-api-key"
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_DEFAULT_MODEL="your-model"
$env:SHOPOPS_MODEL_GATEWAY_REPORT_ENABLED="true"
$env:SHOPOPS_MODEL_GATEWAY_PLANNER_ENABLED="true"
```

相关脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-model-gateway-demo.ps1
powershell -ExecutionPolicy Bypass -File scripts/verify-model-gateway-demo.ps1 -Port 8080
```

## 目录结构

```text
.
├── shopops-admin
│   ├── src/main/java/com/sirithree/shopops/admin
│   ├── src/main/resources
│   └── src/test/java/com/sirithree/shopops/admin
├── shopops-common
├── shopops-admin-ui
│   └── src
├── docs
│   └── demo-data/olist
├── scripts
└── deploy
```

## 关键文档

- [Agent 工作台作品集演示脚本](docs/Agent工作台作品集演示脚本.md)
- [ShopOps 作品集量化报告](docs/ShopOps-portfolio-report.md)
- [ShopOps 公开多源真实数据基线](docs/ShopOps-public-real-baseline.md)
- [ShopOps 简历指标证据表](docs/ShopOps-resume-claim-evidence.md)
- [ShopOps Olist 真实数据基线](docs/ShopOps-olist-real-baseline.md)
- [ShopOps 简历量化基线](docs/ShopOps-resume-baseline.md)
- [ShopOps 作品集中文演示讲稿](docs/ShopOps作品集中文演示讲稿.md)
- [Olist 真实数据接入说明](docs/Olist真实数据接入说明.md)
- [Agent 评测基线与作品集数据](docs/Agent评测基线与作品集数据.md)
- [真实模型网关演示](docs/真实模型网关演示.md)
- [本地开发启动指南](docs/本地开发启动指南.md)
- [文档索引](docs/README.md)

## 当前边界与后续计划

当前边界：

- Olist 在线演示只覆盖订单、评价、商品候选；广告投放、退款/取消代理和外部事件已进入公开多源真实数据基线，但尚未全部接成在线 Connector。
- 当前 Agent Planner 以规则编排为主，真实模型可通过 Model Gateway 接入。
- 当前更偏作品集演示版，距离生产级 SaaS 还需要补充更细的权限模型、监控告警和部署治理。

后续计划：

- 接入更多真实业务数据源，优先补广告投放和售后退款。
- 让 Planner 根据任务意图和店铺配置动态选择工具与步骤。
- 将报告导出到 Excel 或飞书文档。
- 增加更完整的演示数据、截图、架构图和部署文档。
- 继续完善 React 前端的交互细节和可视化展示。

## 面试介绍

可以用这段话概括项目：

> ShopOps 是一个电商运营 AgentOps 平台。我没有只做一个让模型生成日报的 Demo，而是把 Agent 执行拆成任务、步骤、工具、审批、报告、审计、配置和评测。运营人员可以用自然语言发起任务，系统自动路由意图并调用订单、评价、商品等工具，最终生成带业务指标和证据链的运营报告。项目接入了 Olist 公开电商数据，也提供量化评测脚本，用数据证明 Agent 执行、审批和配置逻辑是可验证的。
