# AgentOps 演示与面试讲解脚本

本文档用于把当前 ShopOps Agent 后端治理能力串成一条可展示链路。重点不是单纯展示“生成文本”，而是展示企业 Agent 落地需要的任务、工具、审批、审计、模型网关、外部数据源和报告沉淀。

## 1. 启动服务

推荐使用一键启动脚本，它会准备 Olist 数据、安装公共模块、启动服务并自动打开 Agent 工作台：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops.ps1
```

演示前在另一个 PowerShell 窗口运行健康检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-shopops.ps1
```

如果脚本自动切换了端口，按实际端口检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-shopops.ps1 -Port 8081
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
/admin/workbench.html
/admin/tasks.html
/admin/reports.html
/admin/tools.html
/admin/approvals.html
/admin/audit.html
/admin/prompts.html
/admin/connectors.html
```

讲解节奏：

1. Workbench：从自然语言发起任务，展示意图识别、执行步骤、量化结果和报告入口。
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
当前已经完成 React + TypeScript + Ant Design 主页面迁移、自然语言工作台、用户租户管理、Agent 主链路和治理闭环。仍待产品化的部分主要是真实电商平台 API Connector、生产模型与凭证管理、多级审批策略、长期线上稳定性观测，以及基于真实运营人员计时的效率对照实验。
```
