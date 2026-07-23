# ShopOps Resume Project Description

下面是一版可直接放进简历的 LaTeX 项目描述。所有未实测的耗时收益都明确写成“估算”，不要删掉这个限定词。

```latex
\begin{itemize}
  \resumeItem{\textbf{背景：}面向中小电商团队日常运营中多平台切换、数据复盘耗时长、差评响应滞后和运营建议缺少依据的问题，设计并实现基于 MCP 工具编排的电商运营 AgentOps 平台，支持运营人员通过自然语言发起日报生成、差评分析、商品优化和报表同步等任务。}

  \resumeItem{基于 \textbf{Spring Boot / MyBatis / React / TypeScript / Axios / ECharts / Ant Design} 构建管理后台与 Agent 执行链路，将订单查询、评价分析、商品优化、投放复盘、Excel 导出和飞书同步封装为 \textbf{18 个 MCP 风格工具}，实现任务创建、意图路由、步骤执行、失败重试、降级处理、工具日志和审计追踪。}

  \resumeItem{围绕高风险运营动作设计 \textbf{权限控制、人工审批、确认语校验、店铺运行配置和全链路审计}，使退款执行、商品标题修改和广告预算建议等操作进入审批路由；在公开真实数据基线中从 \textbf{760 个业务样例}派生 \textbf{2670 次工具调用}，其中 \textbf{450 次}高风险调用进入审批链路。}

  \resumeItem{基于 \textbf{Olist、Criteo Attribution、UCI Online Retail 和 Store Sales} 等公开真实数据集构建评测与演示数据，覆盖订单异常、低分评价、商品评价风险、广告低转化、退款/取消代理和外部事件等场景；数据规模包括 \textbf{99441 笔订单、99224 条评价、32951 个商品、16468027 次广告曝光、5947563 次点击、806196 次转化和 541909 行零售交易}。}

  \resumeItem{构建 Agent 量化验收与作品集证据链，自动化评测套件 \textbf{14/14} 个用例通过，工具调用成功率达到 \textbf{98.6\%}，审批决策和配置生效率均为 \textbf{100\%}；离线异常信号评测达到 \textbf{94.81\% Precision、100\% Recall}，并通过 \textbf{100 次}真实飞书 webhook 批量同步验收，HTTP 200 率 \textbf{100\%}、平均耗时 \textbf{311.9ms}。}

  \resumeItem{实现 Markdown 运营日报、证据链展示、真实 \textbf{.xlsx} 报表导出和飞书 webhook 同步闭环；基于 5 个固定运营日报工作流样例估算，Agent 辅助流程将日报整理耗时从约 \textbf{35.4 分钟}降至约 \textbf{4.2 分钟}，预计耗时降低 \textbf{88.14\%}。}
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
