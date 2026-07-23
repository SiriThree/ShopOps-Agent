# ShopOps Resume Baseline

Generated at: 2026-07-23 17:51:18

This baseline is a deterministic resume-oriented simulation built from the ShopOps MCP tool catalog. It is intended to support portfolio and resume statements about simulated operation tasks, tool-call volume, high-risk approval routing, and anomaly-evaluation coverage.

## Summary

| Metric | Value |
|---|---:|
| Simulated operation tasks | 120 |
| Business evaluation samples | 80 |
| MCP tools | 18 |
| Tool calls | 462 |
| Successful tool calls | 447 |
| Failed / degraded tool calls | 15 |
| Tool invocation success rate | 96.8% |
| High-risk tool calls | 23 |
| High-risk calls routed to approval | 23 |
| High-risk approval route rate | 100.0% |
| Manual daily report time | 35 min |
| Agent daily report time | 4 min |
| Anomaly labels | 113 |
| Recalled anomalies | 100 |
| Anomaly recall rate | 88.5% |

## Task Mix

| Task type | Count |
|---|---:|
| daily_review | 38 |
| comment_risk | 28 |
| product_optimization | 24 |
| ad_anomaly | 18 |
| after_sales_risk | 12 |

## Business Samples

| Sample type | Count |
|---|---:|
| order_anomaly | 16 |
| refund_increase | 14 |
| low_score_comment | 20 |
| low_click_product | 16 |
| low_roi_campaign | 14 |

## Tool Calls

| Tool | Calls | High risk |
|---|---:|---|
| `order.query_summary` | 43 | no |
| `order.query_detail` | 20 | no |
| `order.query_refund_risk` | 26 | no |
| `order.refund_execute` | 6 | yes |
| `comment.query_negative` | 41 | no |
| `comment.analyze_sentiment` | 25 | no |
| `comment.create_reply_draft` | 18 | no |
| `product.query_candidates` | 37 | no |
| `product.query_low_click` | 23 | no |
| `product.optimize_title` | 22 | no |
| `product.update_title` | 8 | yes |
| `ad.query_performance` | 39 | no |
| `ad.query_low_roi` | 21 | no |
| `ad.suggest_budget` | 9 | yes |
| `report.query_external_metrics` | 20 | no |
| `report.generate_daily_review` | 50 | no |
| `report.export_excel` | 33 | no |
| `feishu.sync_report` | 21 | no |

## Resume Sentence Supported

在 120 个模拟运营任务中完成 462 次工具调用，高风险操作均进入审批流程，工具调用成功率达到 96.8%。构建店铺经营复盘与差评处理评测集，覆盖订单异常、退款升高、低分评论、低点击商品和低 ROI 投放等 80 个业务样例；相比手工整理流程，单次日报生成耗时从约 35 分钟降至 4 分钟，异常指标召回率达到 88.5%。
