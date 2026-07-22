# ShopOps Agent

ShopOps Agent 是一个面向电商运营场景的 AgentOps 管理后台。它不是单纯的 AI 聊天框，而是把自然语言任务、MCP 风格工具调用、异步任务编排、人工审批、运行配置、运营报告和审计追踪串成一条可治理的 Agent 执行链路。

当前项目最适合作品集展示的主线是：

> 运营人员用自然语言发起日常经营任务，Agent 自动识别意图、编排订单/评价/商品/投放等工具，生成量化运营报告，并沉淀任务、工具、报告、审批和审计链路。

## 当前亮点

- Agent 工作台：支持自然语言输入和一键演示任务。
- 意图路由：支持经营日报、差评专项、商品优化、投放异常。
- 工具编排：统一通过 Tool Gateway 调用工具，记录步骤和工具日志。
- 量化结果：展示 GMV、退款率、差评数、商品候选、广告 ROI 等指标。
- 运行配置：店铺级阈值、审批开关、模型策略可配置并在 Agent 执行中生效。
- 审批风控：高风险工具支持人工审批、确认语校验和审批审计。
- 审计闭环：任务、工具调用、报告、审批、模型调用均可追踪。
- Olist 数据：已支持将 Olist 公开电商数据转换为订单、评价、商品三类 Connector 输入。
- 评测基线：当前 Agent 评测与主流程测试已形成可复现的量化验收。

## 快速体验

启动后端：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

打开 Agent 工作台：

```text
http://localhost:8080/admin/workbench.html
```

工作台里可以直接点击四个演示任务：

| 演示任务 | 预期意图 | 展示重点 |
|---|---|---|
| 经营日报 | `daily_review` | 店铺经营汇总、工具链、量化报告 |
| 差评专项 | `comment_risk` | 风险评价、差评原因、受影响商品 |
| 商品优化 | `product_optimization` | 低点击/待优化商品、优化建议 |
| 投放异常 | `ad_anomaly` | 高消耗低转化计划、ROI 异常 |

## Olist 真实数据演示

准备 Olist demo 文件：

```powershell
python scripts/prepare_olist_demo.py
```

默认配置已经把订单、差评和商品候选三类文件 Connector 指向 `docs/demo-data/olist`。直接启动即可：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

如需替换为其他数据文件，可通过 `SHOPOPS_CONNECTOR_ORDER_SUMMARY_FILE`、`SHOPOPS_CONNECTOR_NEGATIVE_COMMENTS_FILE`、`SHOPOPS_CONNECTOR_PRODUCT_CANDIDATES_FILE` 或同名 Spring 启动参数覆盖。

推荐演示业务日期：

```text
2018-08-07
```

当前 Olist demo 摘要：

| 指标 | 当前结果 |
|---|---:|
| GMV | 62057.77 |
| 订单数 | 370 |
| 售后/退款代理金额 | 4732.62 |
| 售后/退款代理率 | 7.63% |
| 风险评价数 | 51 |
| 商品候选数 | 10 |

说明：Olist 不包含真实退款金额、广告投放和平台外部指标，因此退款使用 `canceled / unavailable` 订单支付金额作为售后风险代理值；广告和外部指标仍使用内置演示数据。

## 量化验收

当前 portfolio baseline：

| 维度 | 结果 |
|---|---:|
| Agent evaluation cases | 14 |
| Passed cases | 14 |
| Completion rate | 100% |
| Tool invocation success rate | 98.6% |
| Approval decision accuracy | 100% |
| Config effect accuracy | 100% |

刷新评测与作品集报告：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-agent-evaluation.ps1
powershell -ExecutionPolicy Bypass -File scripts/generate-portfolio-report.ps1
```

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Maven 多模块
- Spring MVC / Validation
- Spring Security 风格的角色权限控制
- MyBatis / JDBC 持久化路径
- Flyway 数据库迁移
- MySQL、Redis、RabbitMQ 可选开发基础设施

Agent 与平台能力：

- Agent Task Queue
- Rule Planner / Sequential Executor
- MCP-style Tool Registry
- Tool Gateway
- Approval Center
- Audit Timeline
- Report Center
- Model Gateway
- OpenAI-compatible Provider 适配
- Shop Runtime Configuration

前端：

- 当前为 Spring Boot 静态管理页
- Agent 工作台位于 `/admin/workbench.html`
- 后续可迁移到 React + TypeScript + Axios + ECharts + Ant Design

测试与验证：

- JUnit 5
- Spring Boot Test
- H2 / memory persistence 测试模式
- Agent evaluation PowerShell 脚本

## 架构概览

```text
Admin Console / Agent Workbench
        |
        v
Spring Boot Admin Backend
  ├── Auth / Tenant / Shop
  ├── Organization / Shop Config
  ├── Agent Task Queue
  ├── Planner / Executor
  ├── MCP Tool Registry
  ├── Tool Gateway
  ├── Approval Center
  ├── Report Center
  ├── Model Gateway
  └── Audit / Observability
        |
        v
Memory or JDBC Persistence
        |
        v
File Connectors / Olist Demo Data / Model Providers
```

Agent 主链路：

```text
Natural language request
  -> Intent routing
  -> Agent task
  -> Rule planner
  -> Tool gateway
  -> order.query_summary
  -> comment.query_negative
  -> product.query_candidates
  -> ad.query_performance
  -> report.query_external_metrics
  -> report.generate_daily_review
  -> operation report
  -> tool logs / audit timeline
```

## 常用命令

全量测试：

```powershell
mvn -pl shopops-admin -am test
```

工作台与自然语言入口测试：

```powershell
mvn -pl shopops-admin "-Dtest=AdminWorkbenchStaticPageIntegrationTest,AgentNaturalLanguageTaskIntegrationTest" test
```

启动开发基础设施：

```powershell
docker compose -f deploy/docker-compose.dev.yml up -d
```

JDBC / dev profile 启动：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## 关键文档

- [Agent 工作台作品集演示脚本](docs/Agent工作台作品集演示脚本.md)
- [ShopOps 作品集量化报告](docs/ShopOps-portfolio-report.md)
- [ShopOps 作品集中文演示讲稿](docs/ShopOps作品集中文演示讲稿.md)
- [Olist 真实数据接入说明](docs/Olist真实数据接入说明.md)
- [Agent 评测基线与作品集数据](docs/Agent评测基线与作品集数据.md)
- [当前阶段验收与状态](docs/当前阶段验收与状态.md)
- [本地开发启动指南](docs/本地开发启动指南.md)
- [文档索引](docs/README.md)

## 当前边界

- 当前前端仍是静态管理页，还不是 React / TypeScript 工程。
- Olist 只覆盖订单、评价、商品三类真实数据。
- Olist 不提供真实广告投放数据，广告指标当前仍为演示数据。
- Olist 不提供商品标题，当前使用类目英文和 productId 前缀生成展示名。
- 当前默认报告可走规则生成路径，真实模型调用可通过 Model Gateway 配置启用。

## 面试讲法

> ShopOps 是一个电商运营 AgentOps 后台。它的重点不是让模型直接生成一段日报，而是把 Agent 执行拆成任务、步骤、工具、报告、审批、审计、配置和评测。运营人员可以在工作台用自然语言发起任务，系统自动路由意图并编排工具，最终生成带业务指标和证据链的运营报告。项目还接入了 Olist 公开电商数据，并用评测脚本量化验证 Agent 的执行、审批和配置行为。
