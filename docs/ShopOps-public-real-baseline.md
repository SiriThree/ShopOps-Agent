# ShopOps Public Real Data Baseline

Generated at: 2026-07-23 22:34:19

This baseline uses multiple public real datasets to evaluate the ShopOps Agent business coverage and MCP tool routing design. The datasets are not from one merchant, so they should be described as a public multi-source benchmark.

## Data Sources

| Source | Real Records |
|---|---:|
| Olist orders | 99441 |
| Olist reviews | 99224 |
| Olist products | 32951 |
| Criteo impressions | 16468027 |
| Criteo clicks | 5947563 |
| Criteo conversions | 806196 |
| Criteo campaigns | 675 |
| UCI Online Retail lines | 541909 |
| UCI cancellation/refund proxy lines | 10624 |
| UCI cancellation/refund proxy amount | 896812.49 |
| Store Sales holiday events | 350 |

## Benchmark Summary

| Metric | Result |
|---|---:|
| Business samples | 760 |
| MCP tools | 18 |
| Derived tool calls | 2670 |
| High-risk tool calls | 450 |
| Approval-routed high-risk calls | 450 |
| Approval route rate | 100.0% |

## Samples by Dataset

| Dataset | Samples |
|---|---:|
| criteo_attribution | 180 |
| olist | 400 |
| store_sales_holidays | 60 |
| uci_online_retail | 120 |

## Samples by Type

| Sample Type | Samples |
|---|---:|
| ad_low_conversion | 180 |
| delivery_delay_risk | 80 |
| external_event_context | 60 |
| low_score_comment | 90 |
| order_anomaly | 80 |
| product_review_risk | 80 |
| refund_increase | 70 |
| retail_cancel_refund | 120 |

## Tool Calls

| Tool | Calls |
|---|---:|
| report.generate_daily_review | 590 |
| order.query_detail | 280 |
| comment.query_negative | 240 |
| order.query_refund_risk | 190 |
| order.refund_execute | 190 |
| ad.query_performance | 180 |
| ad.query_campaign_detail | 180 |
| ad.suggest_budget | 180 |
| comment.create_reply_draft | 170 |
| comment.analyze_sentiment | 90 |
| order.query_summary | 80 |
| product.query_candidates | 80 |
| product.optimize_title | 80 |
| product.update_title | 80 |
| report.query_external_metrics | 60 |

## Limitations

- The public datasets come from different sources and should be described as a multi-source benchmark, not a single real merchant.
- Store Sales currently only includes holidays_events.csv in the local data folder, so it is used for external event context rather than sales forecasting.
- UCI Online Retail return/after-sales risk is represented by cancellation invoices or negative quantities.
- Criteo campaign identifiers are anonymized, so ShopOps maps them to ad campaign analysis scenarios instead of real product IDs.
