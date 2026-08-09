# Stage 5 Idempotency Expansion Report

## Before → After

| Metric | Baseline | Stage 5 candidate |
|---|---:|---:|
| Dedicated cases | 15 | **22** |
| Semantic scenarios | 9 | **16** |
| Historical held-out scenarios | 6 | **4 candidate held-out** |
| True test-exclusive scenarios | 0 | **4** |
| Cross-split semantic leakage | 6 | **0** |

The held-out count intentionally shrank when contaminated roots were removed; the four Stage 5 test roots are genuinely new semantic scenarios.

## Split

- dev: **11 cases / 6 roots**
- validation: **7 cases / 6 roots**
- test: **4 cases / 4 roots**

## Expansion quality

- Proposed roots: **14**
- Accepted roots: **7**
- Rejected roots: **7**
- Near-duplicate candidates: **82**
- Unresolved high-risk pairs: **0**

## Workload contract

- Dataset semantic scenarios: **16**
- Formal held-out semantic scenarios: **4**
- Formal held-out metric logical operations: **240**
- Formal total logical operations including controls: **260**
- Planned total repeated attempts: **700**

No new held-out Idempotency scenario was executed. Formal runtime evidence remains unavailable.
