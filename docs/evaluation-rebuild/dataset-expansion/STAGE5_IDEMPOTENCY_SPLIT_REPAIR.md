# Stage 5 Idempotency Split Repair

## Baseline

Stage 4 still contained 6 cross-split Idempotency semantic roots. A root is defined by key relation + business-payload relation + repeat/concurrency pattern + fault semantics + expected side-effect semantics. Worker count, request ID, amount-only fixture changes, and seed do not create a new root.

## Repaired roots

| Root | Old cases | Assigned split |
|---|---|---|
| idempotency:baseline_single_delivery | idem-dev-baseline-001, idem-test-baseline-001 | dev |
| idempotency:concurrent_retry | idem-dev-concurrent-retry-001, idem-test-concurrent-retry-001 | dev |
| idempotency:external_success_local_failure | idem-test-external-success-local-failure-001, idem-val-external-success-local-failure-001 | validation |
| idempotency:same_key_different_payload | idem-dev-payload-conflict-001, idem-test-payload-conflict-001 | dev |
| idempotency:sequential_retry | idem-dev-sequential-retry-001, idem-test-sequential-retry-001 | dev |
| idempotency:timeout_after_external_success | idem-dev-timeout-after-success-001, idem-test-timeout-after-success-001 | dev |

All historical test variants were preserved but marked `CONTAMINATED_FOR_HELD_OUT` and `REASSIGNED_STAGE5`; `HELD_OUT` was removed.

## Result

- Old leaked roots: **6**
- Reassigned roots: **6**
- Remaining Idempotency cross-split root leakage: **0**
- Cross-split parent leakage: **0**
- Existing contaminated roots are not recycled as new held-out roots.
