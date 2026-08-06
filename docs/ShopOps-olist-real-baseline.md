# ShopOps Olist Real Data Baseline

Generated at: 2026-07-23 18:13:24

This baseline is generated from the Brazilian E-Commerce Public Dataset by Olist under `data/archive`. It uses real orders, payments, reviews, order items, and product metadata. It is not online production traffic, but the business samples are derived from real public ecommerce records instead of fabricated simulation rows.

## Summary

| Metric | Value |
|---|---:|
| Real orders | 99441 |
| Real reviews | 99224 |
| Real products | 32951 |
| Real-data business samples | 400 |
| MCP tools | 18 |
| Derived tool calls | 1350 |
| Tool routing coverage | 100.0% |
| High-risk tool calls routed to approval | 150 |
| Approval route rate | 100.0% |
| Manual daily report time estimate | 35 min |
| Agent daily report time estimate | 4 min |
| Anomaly labels | 800 |
| Recalled anomalies | 734 |
| Anomaly recall rate | 91.75% |

## Sample Types

| Sample type | Count |
|---|---:|
| order_anomaly | 80 |
| refund_increase | 70 |
| low_score_comment | 90 |
| product_review_risk | 80 |
| delivery_delay_risk | 80 |

## Tool Calls

| Tool | Calls |
|---|---:|
| `order.query_summary` | 80 |
| `order.query_detail` | 160 |
| `report.generate_daily_review` | 230 |
| `order.query_refund_risk` | 70 |
| `comment.query_negative` | 240 |
| `order.refund_execute` | 70 |
| `comment.analyze_sentiment` | 90 |
| `comment.create_reply_draft` | 170 |
| `product.query_candidates` | 80 |
| `product.optimize_title` | 80 |
| `product.update_title` | 80 |

## Limitations

- Olist does not contain real advertising impressions, clicks, cost, or ROI.
- Olist does not contain real product click-through-rate data.
- Refund is represented by canceled/unavailable order payment amount as an after-sales risk proxy.

## Resume Sentence Supported

Based on the Olist public ecommerce dataset, ShopOps constructs 400 real-data business evaluation samples from 99441 orders, 99224 reviews, and 32951 products, covering order anomalies, refund/after-sales proxy increases, low-score comments, product review risk, and delivery-delay risk. The derived Agent workflow triggers 1350 MCP tool calls, routes all high-risk actions to approval, and reaches 91.75% anomaly recall under a fixed evaluation protocol.
