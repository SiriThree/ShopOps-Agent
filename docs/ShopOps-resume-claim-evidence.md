# ShopOps Resume Claim Evidence

Generated at: 2026-07-23 23:35:03

This file separates resume-safe metrics from metrics that still need real experiments or third-party API integration.

## Summary

| Metric | Value |
|---|---:|
| Total claims | 14 |
| Verified claims | 7 |
| Not verified claims | 7 |

## Claims

| Key | Status | Value / Reason |
|---|---|---|
| public_real_data_scale | VERIFIED | {"olistOrders": 99441, "olistReviews": 99224, "olistProducts": 32951, "criteoImpressions": 16468027, "criteoClicks": 5947563, "criteoConversions": 806196, "uciRetailLines": 541909, "storeHolidayEvents": 350} |
| real_business_sample_count | VERIFIED | 760 |
| derived_mcp_tool_calls | VERIFIED | 2670 |
| high_risk_approval_routed_calls | VERIFIED | 450 |
| low_roi_ad_detection_proxy | VERIFIED | {"labeledAdRiskSamples": 180, "routedAdRiskSamples": 180, "routingRecall": 100.0} |
| agent_evaluation_suite | VERIFIED | {"caseCount": 14, "passedCaseCount": 14, "toolInvocationSuccessRate": 98.6, "approvalDecisionAccuracy": 100, "configEffectAccuracy": 100, "avgTaskDurationMs": 33.8} |
| excel_real_xlsx_export | VERIFIED | {"filePath": "docs/evaluation/shopops-operation-report-sample.xlsx", "sourceFilePath": "shopops-admin/target/shopops-exports/shopops-operation-report-XLSX-20260723231649512.xlsx", "fileSizeBytes": 3744, "worksheetCount": 4} |
| manual_35min_to_agent_4min | NOT_VERIFIED | 当前没有至少 5 次人工手工流程计时记录，不能把机器侧耗时换算成运营人员节省时间。 |
| old_120_simulated_tasks | NOT_VERIFIED | 当前真实基线是 760 个公开真实数据业务样例；旧的 120 模拟任务数字没有对应现存复跑产物，不建议继续使用。 |
| old_462_tool_calls | NOT_VERIFIED | 当前可复跑的公开真实基线为 2670 次派生 MCP 工具调用；旧 462 没有对应现存复跑产物。 |
| old_96_8_tool_success_rate | NOT_VERIFIED | 当前 Maven 自动化评测真实产物中的工具调用成功率为 98.6%；旧 96.8% 不建议继续使用。 |
| feishu_real_sync_success_rate | NOT_VERIFIED | 当前 feishu.sync_report 是 demo connector，返回 feishu.example.com，不是飞书开放平台真实 API 调用。 |
| excel_real_export_time_saving | NOT_VERIFIED | 当前已经能生成真实 xlsx 文件，但还没有人工整理 Excel 的计时对比，因此不能写“节省多少时间”。 |
| anomaly_recall_88_5 | NOT_VERIFIED | 旧 88.5% 没有独立预测结果与真实标签对比产物。当前可以引用 Criteo 广告风险工具路由召回，但不能写成异常检测召回率。 |

## Resume-Safe Wording

```latex
\resumeItem{基于 \textbf{Olist、Criteo Attribution、UCI Online Retail 和 Store Sales} 等公开真实数据集构建多源运营评测基线，覆盖订单异常、低分评价、商品评价风险、广告低转化、退款/取消代理和外部事件等场景；从 \textbf{99441 笔订单、99224 条评价、32951 个商品、16468027 次广告曝光、5947563 次点击、806196 次转化和 541909 行零售交易} 中生成 \textbf{760 个真实数据业务样例}。}

\resumeItem{将真实数据样例映射到 \textbf{18 个 MCP 工具}的 Agent 执行链路，派生 \textbf{2670 次工具调用}，其中 \textbf{450 次}退款执行、商品标题修改和广告预算建议等高风险动作进入审批路由；自动化评测套件当前 \textbf{14/14} 个 Agent 链路用例通过，工具调用成功率达到 \textbf{98.6\%}，审批决策和配置生效率均为 \textbf{100\%}。}
```

Do not claim the old 35-to-4-minute, 88.5% anomaly recall, real Feishu sync, or real Excel time-saving metrics until the required experiments are implemented and recorded.
