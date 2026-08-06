# ShopOps Resume Baseline

Generated at: 2026-07-23 17:56:36

This baseline is a deterministic resume-oriented simulation built from the ShopOps MCP tool catalog. It is intended to support portfolio and resume statements about simulated operation tasks, tool-call volume, high-risk approval routing, and anomaly-evaluation coverage. It is not online production traffic.

## Summary

| Metric | Value |
|---|---:|
| Simulated operation tasks | 500 |
| Business evaluation samples | 300 |
| MCP tools | 18 |
| Tool calls | 1836 |
| Successful tool calls | 1783 |
| Failed / degraded tool calls | 53 |
| Tool invocation success rate | 97.1% |
| High-risk tool calls | 92 |
| High-risk calls routed to approval | 92 |
| High-risk approval route rate | 100.0% |
| Manual daily report time | 35 min |
| Agent daily report time | 4 min |
| Anomaly labels | 426 |
| Recalled anomalies | 381 |
| Anomaly recall rate | 89.4% |

## Task Mix

| Task type | Count |
|---|---:|
| daily_review | 160 |
| comment_risk | 115 |
| product_optimization | 95 |
| ad_anomaly | 75 |
| after_sales_risk | 55 |

## Business Samples

| Sample type | Count |
|---|---:|
| order_anomaly | 60 |
| refund_increase | 55 |
| low_score_comment | 70 |
| low_click_product | 60 |
| low_roi_campaign | 55 |

## Tool Calls

| Tool | Calls | High risk |
|---|---:|---|
| `order.query_summary` | 168 | no |
| `order.query_detail` | 76 | no |
| `order.query_refund_risk` | 104 | no |
| `order.refund_execute` | 24 | yes |
| `comment.query_negative` | 160 | no |
| `comment.analyze_sentiment` | 98 | no |
| `comment.create_reply_draft` | 72 | no |
| `product.query_candidates` | 148 | no |
| `product.query_low_click` | 92 | no |
| `product.optimize_title` | 88 | no |
| `product.update_title` | 32 | yes |
| `ad.query_performance` | 152 | no |
| `ad.query_low_roi` | 84 | no |
| `ad.suggest_budget` | 36 | yes |
| `report.query_external_metrics` | 84 | no |
| `report.generate_daily_review` | 204 | no |
| `report.export_excel` | 133 | no |
| `feishu.sync_report` | 81 | no |

## Resume Sentence Supported

In 500 simulated operation tasks, ShopOps completed 1836 tool calls. All high-risk actions were routed to approval, and the tool invocation success rate reached 97.1%. The simulated business evaluation set contains 300 samples across order anomalies, refund increases, low-score comments, low-click products, and low-ROI campaigns. Under the fixed evaluation protocol, daily report generation time is estimated to drop from 35 minutes manually to 4 minutes through the Agent workflow, and anomaly recall reaches 89.4%.
