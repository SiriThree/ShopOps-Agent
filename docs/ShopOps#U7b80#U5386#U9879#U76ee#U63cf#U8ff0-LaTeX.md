# ShopOps Resume Project Description

下面是一版可直接放进简历的短版 LaTeX 项目描述，控制在 500 字以内。耗时收益未放入主描述，避免和实测指标混用。

```latex
\begin{itemize}
  \resumeItem{\textbf{背景：}面向中小电商团队多平台复盘耗时、差评响应滞后和运营建议缺少依据的问题，设计并实现电商运营 AgentOps 平台，支持运营人员通过自然语言发起日报生成、差评分析、商品优化和报表同步等日常任务。}

  \resumeItem{围绕“自然语言任务到运营动作”的闭环构建 Agent 工作台与 MCP 风格工具层，将订单查询、评价分析、商品优化、投放复盘、Excel 导出和飞书同步封装为 \textbf{18 个工具}；后端基于 Spring Boot 实现意图路由、步骤执行、失败重试、降级处理和 trace 追踪，前端提供执行过程与报告可视化。}

  \resumeItem{针对退款执行、商品标题修改和广告预算建议等高风险动作，设计\textbf{权限控制、人工审批、确认语校验、店铺运行配置和全链路审计}，将 Agent 的自动化能力限制在可追踪、可回滚、可人工确认的治理框架内；基于公开真实数据构建 \textbf{760 个业务样例}，派生 \textbf{2670 次工具调用}，其中 \textbf{450 次}进入审批链路。}

  \resumeItem{基于公开真实的\textbf{电商订单、用户评价、广告归因、零售交易和节假日事件数据}构建多源评测，覆盖订单异常、低分评价、低转化广告和退款/取消代理等场景；自动化评测 \textbf{14/14} 通过，工具调用成功率 \textbf{98.6\%}，异常信号评测达到 \textbf{94.81\% Precision、100\% Recall}，并完成 \textbf{100 次}真实飞书 webhook 同步验收。}
\end{itemize}
```

## Claim Boundary

| Claim | Status | Source |
|---|---|---|
| 760 real-data business samples | VERIFIED | `docs/ShopOps-public-real-baseline.json` |
| 2670 derived MCP tool calls | VERIFIED | `docs/ShopOps-public-real-baseline.json` |
| 450 high-risk approval-routed calls | VERIFIED | `docs/ShopOps-public-real-baseline.json` |
| 14/14 Agent evaluation cases | VERIFIED | `shopops-admin/target/evaluation/agent-eval-portfolio-summary.json` |
| 98.6% tool invocation success rate | VERIFIED | `docs/ShopOps-resume-claim-evidence.md` |
| 94.81% Precision / 100% Recall anomaly signal evaluation | VERIFIED | `docs/ShopOps-real-anomaly-evaluation.md` |
| 100 Feishu webhook calls, 100% HTTP 200 | VERIFIED | `docs/evaluation/feishu-webhook-batch-summary.json` |
| 35.4 min -> 4.2 min time saving | ESTIMATED | `docs/evaluation/manual-report-timing-estimated.csv` |

不要把最后一条写成真实人工计时结果；它目前是固定流程估算。
