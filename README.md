# ShopOps

ShopOps 是一个面向电商运营场景的 AgentOps 管理平台。项目目标是把运营人员的自然语言任务，拆解成可编排、可审计、可审批、可配置、可评测的 Agent 执行链路。



> 运营人员在 Agent 工作台用自然语言发起日常任务，系统自动识别意图，调用订单、评价、商品、投放等工具，生成结构化运营日报、异常告警和改进建议，并沉淀任务、工具、报告、审批、审计和量化评测数据。

## 可验证结果

| 指标 | 当前结果 | 证据 |
|---|---:|---|
| 公开真实数据业务样例 | 760 | Olist、Criteo、UCI Online Retail、Store Sales |
| 派生 MCP 工具调用 | 2670 | `docs/ShopOps-public-real-baseline.json` |
| 高风险审批路由调用 | 450 | 退款、商品标题、广告预算建议 |
| Agent 自动化评测 | 14/14 通过 | `shopops-admin/target/evaluation` |
| 工具调用成功率 | 98.6% | `docs/ShopOps-resume-claim-evidence.md` |
| 异常信号评测 | Precision 94.81%, Recall 100% | `docs/ShopOps-real-anomaly-evaluation.md` |
| 飞书 webhook 批量验收 | 100/100 成功，HTTP 200 率 100% | `docs/evaluation/feishu-webhook-batch-summary.json` |
| 飞书 webhook 平均耗时 | 311.9 ms | 100 次连续真实 webhook 调用 |
| Excel 报表导出 | 真实 `.xlsx`，4 个 worksheet | `docs/evaluation/shopops-operation-report-sample.xlsx` |
| 日报耗时收益 | 估算 35.4 分钟 -> 4.2 分钟 | 标记为 `ESTIMATED`，不是实测人工计时 |

## 项目亮点

- 自然语言 Agent 工作台：运营人员可以输入“生成今天店铺运营日报”“分析最近差评原因”“找出低点击商品并给优化建议”等任务，系统自动路由并展示执行步骤、工具调用和最终报告。
- 标准 MCP 工具服务：提供 HTTP JSON-RPC、stdio 与 SSE transport，支持 `server/discover`、`initialize`、`tools/list`、`tools/call`，并将订单查询、评价分析、商品优化、投放复盘、Excel 导出、飞书同步等 18 个工具暴露为 MCP tools。
- Agent 执行闭环：支持任务创建、意图路由、步骤执行、同步/异步调度、失败重试、降级处理、任务追踪和报告落库。
- 风控与人工审批：退款执行、商品标题修改、广告预算建议等高风险工具进入审批流程，支持确认语、撤回、批量处理、过期处理和审计追踪。
- 店铺运行配置：退款率阈值、差评阈值、审批开关、模型策略可按店铺配置，并在 Agent 报告和工具执行中真实生效。
- 真实数据与评测：Olist 作为在线演示主链路，Criteo、UCI Online Retail、Store Sales 补齐广告、退款/取消代理和外部事件评测缺口。
- 报表输出闭环：运营日报可查看 Markdown 证据链，可导出真实 Excel 文件，并已通过 100 次真实飞书 webhook 批量同步验收。
- React 管理前端：后台已迁移到 React + TypeScript + Axios + ECharts + Ant Design，覆盖工作台、任务、报告、审计、工具、审批、模型和组织配置等页面。
- 可复现作品集证据：提供基线生成、Agent 评测、异常信号评测、飞书批量验收、耗时收益估算等脚本和文档。

## 当前完成度

| 模块 | 状态 | 说明 |
|---|---|---|
| Agent 工作台 | 已完成 v1 | 自然语言输入、快捷任务、Olist 演示、执行步骤、量化结果、报告入口 |
| Agent 任务流 | 已完成 | 创建、运行、重试、步骤、事件、任务详情、异步/同步执行 |
| MCP 工具中心 | 已完成 v1 | 标准 `/mcp` JSON-RPC 入口、独立 stdio 进程、SSE transport、工具注册、工具网关、调用日志、失败统计、工具审批状态 |
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

一键启动演示：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops.ps1
```

脚本会自动准备 Olist 演示数据、安装 `shopops-common`、检查/启动本地 MySQL 持久化存储、启动后端，并在启动成功后打开工作台。若 `8080` 被占用，会自动尝试 `8081` 到 `8100`。

如果只安装了 Docker Desktop，可以直接构建、启动并预置完整演示链路：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops-docker.ps1
```

Docker Hub 访问受限时：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops-docker.ps1 -UseChinaMirror
```

停止容器：

```powershell
docker compose -p shopops-demo -f deploy/docker-compose.demo.yml down
```

启动后访问：

```text
http://localhost:8080/admin/workbench.html
```

如果脚本自动切换了端口，以脚本输出的 `Workbench` 地址为准。

常用选项：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops.ps1 -Port 8081
powershell -ExecutionPolicy Bypass -File scripts/start-shopops.ps1 -NoOpenBrowser
powershell -ExecutionPolicy Bypass -File scripts/start-shopops.ps1 -StrictPort
powershell -ExecutionPolicy Bypass -File scripts/start-shopops.ps1 -Memory
```

演示前健康检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-shopops.ps1
```

一键预置完整演示链路：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/seed-shopops-demo.ps1
```

该脚本会等待服务就绪、执行健康检查，并创建经营日报任务、报告、高风险工具审批、审批后重试和审计记录。完成后会输出任务、报告、工具、审批、审计页面地址并打开工作台。默认 JDBC / MySQL 模式下，任务、报告、工具日志、审批和审计记录会保留；如需临时清空式演示，可用 `start-shopops.ps1 -Memory`。

如果后端不在 8080：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-shopops.ps1 -Port 8081
powershell -ExecutionPolicy Bypass -File scripts/seed-shopops-demo.ps1 -Port 8081
```

也可以手动分步启动：

准备 Olist 演示数据：

```powershell
python scripts/prepare_olist_demo.py
```

启动后端：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--server.port=8080"
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
  ├── MCP Server / Tool Registry
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

- MCP Server / Tool Registry
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

GitHub Actions 会在每次 push 和 Pull Request 时并行执行后端测试、React 前端构建以及 Docker Compose/镜像构建，工作流位于 `.github/workflows/ci.yml`。

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
| Real anomaly signal precision | 94.81% |
| Real anomaly signal recall | 100% |
| Feishu webhook batch success rate | 100/100 |
| Feishu webhook HTTP 200 rate | 100% |
| Feishu webhook average latency | 311.9 ms |
| Estimated daily report time saving | 35.4 min -> 4.2 min |
| 最近全量测试 | 88 tests, 0 failures, 8 skipped |

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
- 飞书 webhook 真实 HTTP 同步
- Excel 报表真实导出

说明：`Estimated daily report time saving` 来自固定工作流估算基线，证据项标记为 `ESTIMATED`；真实人工计时仍需补充至少 5 次人工流程记录。

## MCP Server

ShopOps 已提供标准 MCP 工具服务入口：

- Endpoint：`POST /mcp`
- Transport：HTTP JSON-RPC
- Protocol header：响应会返回 `MCP-Protocol-Version: 2026-07-28`
- 支持方法：`server/discover`、`initialize`、`tools/list`、`tools/call`
- 上下文：沿用后台 Header 鉴权和租户上下文，例如 `X-Tenant-Id`、`X-Shop-Id`、`X-User-Id`、`X-User-Roles`
- 执行链路：`tools/call` 复用现有 `ToolGatewayService`，因此工具日志、审批、店铺配置和审计链路继续生效

Transport：

- HTTP JSON-RPC：`POST /mcp`
- HTTP with SSE：`GET /mcp/sse` 建立 SSE 会话，`POST /mcp/messages?sessionId=...` 投递 JSON-RPC 消息
- stdio：`scripts/run-mcp-stdio-server.ps1`

示例：

```powershell
$headers = @{
  "Content-Type" = "application/json"
  "X-Tenant-Id" = "1"
  "X-Shop-Id" = "1"
  "X-User-Id" = "1"
  "X-User-Roles" = "ADMIN"
}

$body = @{
  jsonrpc = "2.0"
  id = "tools-1"
  method = "tools/list"
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/mcp" -Method Post -Headers $headers -Body $body
```

一键验收 MCP Server：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-mcp-server.ps1
```

输出证据：

- `docs/evaluation/mcp-server-verification-summary.json`
- `docs/evaluation/mcp-server-verification-summary.md`

独立 stdio MCP Server 进程：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-mcp-stdio-server.ps1
```

MCP Client 配置示例：

```json
{
  "mcpServers": {
    "shopops": {
      "command": "powershell",
      "args": [
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "D:\\找实习\\ShopOps\\scripts\\run-mcp-stdio-server.ps1"
      ]
    }
  }
}
```

说明：stdio 进程默认以 `memory` 模式启动，便于 MCP Client 直接拉起；后台管理端仍可通过一键启动脚本使用 JDBC/MySQL 持久化。

## Model Gateway

Model Gateway 默认开启，并优先尝试 OpenAI-compatible provider 参与 Planner 与 Report 生成；本地没有真实 API Key 或模型服务不可用时，会自动降级到规则链路，保证演示可继续。

推荐复制 `.env.example` 为 `.env`，然后在 `.env` 中填写本地 API Key；`.env` 已被 Git 忽略，不会提交到仓库：

```dotenv
SHOPOPS_MODEL_OPENAI_COMPATIBLE_BASE_URL=https://api.deepseek.com/v1
SHOPOPS_MODEL_OPENAI_COMPATIBLE_API_KEY=your-api-key
SHOPOPS_MODEL_OPENAI_COMPATIBLE_DEFAULT_MODEL=deepseek-chat
```

之后直接运行一键启动脚本即可，脚本会自动读取项目根目录的 `.env`。

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
- [ShopOps 真实异常信号评测](docs/ShopOps-real-anomaly-evaluation.md)
- [ShopOps 简历指标证据表](docs/ShopOps-resume-claim-evidence.md)
- [ShopOps 简历项目描述 LaTeX](docs/ShopOps简历项目描述-LaTeX.md)
- [ShopOps 日报耗时证据表](docs/ShopOps-operation-timing-evidence.md)
- [ShopOps Olist 真实数据基线](docs/ShopOps-olist-real-baseline.md)
- [ShopOps 简历量化基线](docs/ShopOps-resume-baseline.md)
- [Feishu 同步 Webhook 验收说明](docs/Feishu同步Webhook验收说明.md)
- [Feishu 批量同步验收记录](docs/Feishu批量同步验收记录.md)
- [ShopOps 作品集中文演示讲稿](docs/ShopOps作品集中文演示讲稿.md)
- [Olist 真实数据接入说明](docs/Olist真实数据接入说明.md)
- [Agent 评测基线与作品集数据](docs/Agent评测基线与作品集数据.md)
- [真实模型网关演示](docs/真实模型网关演示.md)
- [本地开发启动指南](docs/本地开发启动指南.md)
- [文档索引](docs/README.md)


