# Stage 5 Idempotency Workload Rationale

The FORMAL allocation is risk-weighted, not quota-driven. Four new test-exclusive semantic scenarios receive 60 logical operations each. Two contaminated baseline controls receive 10 logical operations each and are explicitly excluded from held-out metric counts.

## Held-out allocation

| Scenario | Logical ops | Attempts/op | Workers |
|---|---|---|---|
| idempotency:concurrent_timeout_after_acceptance | 60 | 5 | 5 |
| idempotency:payload_conflict_after_timeout_after_acceptance | 60 | 2 | 1 |
| idempotency:payload_conflict_after_terminal_external_failure | 60 | 2 | 1 |
| idempotency:payload_conflict_after_response_loss_commit | 60 | 2 | 1 |

## Controls

| Scenario | Logical ops | Attempts/op | Workers |
|---|---|---|---|
| idempotency:baseline_single_delivery | 10 | 1 | 1 |
| idempotency:sequential_retry | 10 | 3 | 1 |

## Planned totals

- Held-out metric logical operations: **240**
- Control logical operations: **20**
- Total logical operations: **260**
- Held-out planned attempts: **660**
- Total planned attempts: **700**
- Held-out expected effective effects: **180**
- Total expected effective effects including controls: **200**

The allocation does not use extreme retry counts simply to inflate attempt totals. Worker count is workload intensity, not semantic identity. The workload is designed but not Formal-ready until the approval-reuse benchmark-driver gap is hardened.
