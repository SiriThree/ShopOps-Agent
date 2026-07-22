# AgentOps 演示与面试讲解脚本

本文档用于把当前 ShopOps Agent 后端治理能力串成一条可展示链路。重点不是单纯展示“生成文本”，而是展示企业 Agent 落地需要的任务、工具、审批、审计、模型网关、外部数据源和报告沉淀。

## 1. 启动服务

默认 memory 模式即可演示完整链路：

```powershell
mvn -pl shopops-admin spring-boot:run
```

如果 8080 被占用，可以换端口：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

如果要演示外部业务数据源，可以使用示例 JSON 文件启动：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--shopops.connector.order-summary.file=docs/demo-data/order-summary-demo.json --shopops.connector.negative-comments.file=docs/demo-data/negative-comments-demo.json --shopops.connector.product-candidates.file=docs/demo-data/product-candidates-demo.json --shopops.connector.ad-performance.file=docs/demo-data/ad-performance-demo.json --shopops.connector.external-reports.file=docs/demo-data/external-reports-demo.json"
```

## 2. 一键验收演示

服务启动后，在另一个 PowerShell 窗口执行：

```powershell
.\scripts\verify-agentops-demo.ps1 -Port 8080
```

脚本会依次验证 Dashboard、创建 `daily_review` 任务、读取报告、触发高风险退款工具、审批拦截、二次确认、审批后重试工具，以及 Audit Center 时间线。

## 3. 前端演示顺序

推荐按这个顺序打开页面：

```text
/admin/dashboard.html
/admin/tasks.html
/admin/reports.html
/admin/tools.html
/admin/approvals.html
/admin/audit.html
/admin/prompts.html
/admin/connectors.html
```

讲解节奏：

1. Dashboard：先展示任务、报告、工具调用、失败和健康状态。
2. Tasks：展示 Agent 任务生命周期、步骤、Report ID、Trace ID 和事件流。
3. Reports：展示每日经营复盘报告，说明报告由工具证据链生成。
4. Tools：展示工具注册表、权限、风险等级、调用状态和失败记录。
5. Approvals：演示高风险退款进入审批，输入 `确认通过` 后才允许继续执行。
6. Audit：按 `APPROVAL`、`TOOL`、`CONNECTOR` 来源筛选，展示统一审计时间线。
7. Prompts：展示 Prompt 模板版本、启用版本和渲染测试。
8. Connectors：展示外部文件数据源配置、可用性、凭证掩码、轮换提醒、同步任务和外部调用日志。

## 4. 面试讲法

可以这样概括项目：

```text
ShopOps Agent 不是一个单纯的大模型报告 demo，而是面向电商运营的 AgentOps 后台。它把自然语言经营复盘任务拆成可审计的工具调用链路，通过 MCP Tool Gateway 控制工具权限和风险等级。高风险动作不会直接执行，而是生成审批单，经过角色权限、二次确认和审计记录后，才能携带 approvalId 继续执行。模型调用统一进入 Model Gateway，支持 provider 抽象、Prompt 模板版本、调用日志、超时重试和失败降级。最终所有任务、工具、审批、模型、连接器和报告都会进入 Dashboard 与 Audit Center，形成可观测、可追踪、可治理的企业 Agent 闭环。
```

## 5. 亮点回答

和普通 AI 应用的区别：

```text
普通 AI 应用更关注生成结果；这个项目更关注企业可控性。我把 Agent 执行拆成任务、步骤、工具调用、模型调用、审批和审计事件，每个动作都能追踪、复盘和限制权限。尤其是高风险退款工具，系统会先返回 APPROVAL_REQUIRED，审批通过且带二次确认后才允许执行。
```

为什么需要 Model Gateway：

```text
Model Gateway 负责屏蔽不同模型供应商差异，并统一处理调用日志、Prompt 版本、超时、重试和降级。业务代码不直接依赖某一个模型接口，后续切换 OpenAI-compatible 服务、Ollama 或其他模型时，只需要改 provider 配置。
```

目前还没做什么：

```text
当前重点完成的是后端治理闭环和静态管理台演示。待完成方向包括 React / Ant Design Pro 产品化前端、用户租户管理页面、多级审批、按金额阈值配置审批策略，以及 Prompt 模板高级编辑能力。
```
