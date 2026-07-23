# ShopOps Real Anomaly Evaluation

Generated at: 2026-07-24 01:07:59

This evaluation runs a fixed rule detector over the public real-data business samples and compares predicted anomaly signals against expected signals.

## Summary

| Metric | Value |
|---|---:|
| Samples | 760 |
| Passed samples | 760 |
| Sample pass rate | 100.0% |
| Expected signals | 1460 |
| Predicted signals | 1540 |
| Matched signals | 1460 |
| Precision | 94.81% |
| Recall | 100.0% |
| F1 | 97.34 |

## By Sample Type

| Sample Type | Samples | Precision | Recall | F1 |
|---|---:|---:|---:|---:|
| ad_low_conversion | 180 | 100.0% | 100.0% | 100.0 |
| delivery_delay_risk | 80 | 66.67% | 100.0% | 80.0 |
| external_event_context | 60 | 100.0% | 100.0% | 100.0 |
| low_score_comment | 90 | 100.0% | 100.0% | 100.0 |
| order_anomaly | 80 | 100.0% | 100.0% | 100.0 |
| product_review_risk | 80 | 100.0% | 100.0% | 100.0 |
| refund_increase | 70 | 100.0% | 100.0% | 100.0 |
| retail_cancel_refund | 120 | 100.0% | 100.0% | 100.0 |

## By Signal

| Signal | Expected | Predicted | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|
| ad_spend_risk | 180 | 180 | 100.0% | 100.0% | 100.0 |
| after_sales_risk | 190 | 190 | 100.0% | 100.0% | 100.0 |
| customer_experience_risk | 170 | 170 | 100.0% | 100.0% | 100.0 |
| delivery_delay | 80 | 80 | 100.0% | 100.0% | 100.0 |
| external_event_context | 60 | 60 | 100.0% | 100.0% | 100.0 |
| gmv_anomaly | 80 | 80 | 100.0% | 100.0% | 100.0 |
| low_conversion_risk | 180 | 180 | 100.0% | 100.0% | 100.0 |
| low_score_review | 90 | 170 | 52.94% | 100.0% | 69.23 |
| order_volume_anomaly | 80 | 80 | 100.0% | 100.0% | 100.0 |
| product_optimization_candidate | 80 | 80 | 100.0% | 100.0% | 100.0 |
| product_review_risk | 80 | 80 | 100.0% | 100.0% | 100.0 |
| refund_proxy_increase | 190 | 190 | 100.0% | 100.0% | 100.0 |

## Limitations

- This is an offline rule-based evaluation over public benchmark samples, not online production monitoring.
- Predicted signals are generated from sample metrics only; model-generated narratives are not included.
- Criteo campaign IDs are anonymized, so ad risks are evaluated at campaign benchmark level.
