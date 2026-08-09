# Stage 5 Idempotency Semantic Matrix

| Root | Key relation | Payload relation | Repeat pattern | Fault | Expected effects | Split |
|---|---|---|---|---|---|---|
| idempotency:baseline_single_delivery | SAME_KEY | SAME_PAYLOAD | SINGLE | NONE | 1 | dev |
| idempotency:concurrent_external_success_local_failure | SAME_KEY | SAME_PAYLOAD | CONCURRENT_FIRST_WRITE | EXTERNAL_SUCCESS_LOCAL_PERSIST_FAILURE | 1 | dev |
| idempotency:concurrent_retry | SAME_KEY | SAME_PAYLOAD | CONCURRENT_FIRST_WRITE | NONE | 1 | dev |
| idempotency:concurrent_timeout_after_acceptance | SAME_KEY | SAME_PAYLOAD | CONCURRENT_FIRST_WRITE | TIMEOUT_AFTER_ACCEPTANCE | 1 | test |
| idempotency:external_idempotent_comparison | SAME_KEY | SAME_PAYLOAD | SEQUENTIAL_RETRY | IDEMPOTENT_EXTERNAL_CONTROL | 1 | validation |
| idempotency:external_success_local_failure | SAME_KEY | SAME_PAYLOAD | SEQUENTIAL_RETRY | EXTERNAL_SUCCESS_LOCAL_PERSIST_FAILURE | 1 | validation |
| idempotency:payload_conflict_after_external_success_local_failure | SAME_KEY | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_LOCAL_FAILURE | EXTERNAL_SUCCESS_LOCAL_PERSIST_FAILURE | 1 | validation |
| idempotency:payload_conflict_after_response_loss_commit | SAME_KEY | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_COMMIT_RESPONSE_LOSS | RESPONSE_LOSS_AFTER_LOCAL_COMMIT | 1 | test |
| idempotency:payload_conflict_after_terminal_external_failure | SAME_KEY | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_TERMINAL_FAILURE | EXTERNAL_BUSINESS_FAILURE | 0 | test |
| idempotency:payload_conflict_after_timeout_after_acceptance | SAME_KEY | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_UNCERTAIN_RESULT | TIMEOUT_AFTER_ACCEPTANCE | 1 | test |
| idempotency:payload_conflict_after_timeout_before_acceptance | SAME_KEY | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_UNCERTAIN_RESULT | TIMEOUT_BEFORE_ACCEPTANCE | 0 | validation |
| idempotency:response_loss_after_local_confirm | SAME_KEY | SAME_PAYLOAD | SEQUENTIAL_RETRY | RESPONSE_LOSS_AFTER_LOCAL_COMMIT | 1 | validation |
| idempotency:same_key_different_payload | SAME_KEY | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT | NONE | 1 | dev |
| idempotency:sequential_retry | SAME_KEY | SAME_PAYLOAD | SEQUENTIAL_RETRY | NONE | 1 | dev |
| idempotency:timeout_after_external_success | SAME_KEY | SAME_PAYLOAD | SEQUENTIAL_RETRY | TIMEOUT_AFTER_ACCEPTANCE | 1 | dev |
| idempotency:timeout_before_external_success | SAME_KEY | SAME_PAYLOAD | SEQUENTIAL_RETRY | TIMEOUT_BEFORE_ACCEPTANCE | 1 | validation |

## Summary

- Semantic scenarios: **16**
- `SAME_KEY`: **16 roots**
- `SAME_PAYLOAD`: **10 roots**
- `DIFFERENT_BUSINESS_PAYLOAD`: **6 roots**
- Current dataset does **not** claim coverage for `SAME_BUSINESS_DIFFERENT_METADATA` or `DIFFERENT_KEY_SAME_BUSINESS_TARGET`; both remain explicit contract gaps.

Semantic identity is independent of workload size. A 60-operation formal allocation still represents one semantic root.
