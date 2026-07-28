# ShopOps Agent Natural Language Batch Evaluation

- Generated at: 2026-07-28 20:12:45
- Base URL: http://localhost:8080
- Date range: 2018-08-01 to 2018-08-07 (7 days)
- Rounds: 10

## Summary

| Metric | Value |
|---|---:|
| Cases | 280 |
| Passed cases | 280 |
| Pass rate | 100% |
| Success rate | 100% |
| Intent accuracy | 100% |
| Tool invocations | 1260 |
| Tool invocation success rate | 100% |
| Avg tools per task | 4.5 |
| Avg wall-clock duration | 646.8 ms |
| P95 wall-clock duration | 863.5 ms |
| Avg task duration | 514.3 ms |

## Scenario Breakdown

| Scenario | Cases | Passed | Success Rate | Avg Tools | Avg Duration ms |
|---|---:|---:|---:|---:|---:|
| ad_anomaly | 70 | 70 | 100% | 4 | 598.1 |
| comment_risk | 70 | 70 | 100% | 4 | 606.6 |
| daily_review | 70 | 70 | 100% | 6 | 783.3 |
| product_optimization | 70 | 70 | 100% | 4 | 599.1 |

## Output Files

- JSON summary: D:\找实习\ShopOps\docs\evaluation\agent-natural-language-batch-summary.json
- CSV details: D:\找实习\ShopOps\docs\evaluation\agent-natural-language-batch-details.csv

## Notes

- This batch calls the real ShopOps natural-language task API.
- The backend must be started with the configured public-data file connectors.
- wallClockDurationMs is measured by this runner around the HTTP task creation and verification flow.
- taskDurationMs is computed from ShopOps task startedAt and finishedAt fields when available.
