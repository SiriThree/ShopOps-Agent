# Stage 5 Idempotency Scenario Blueprints

## Author → Feasibility → Critic result

- Proposed: **14**
- Accepted: **7**
- Rejected: **7**

### Accepted roots

| Root | Split | Payload relation | Repeat | Fault | Expected effects |
|---|---|---|---|---|---|
| idempotency:concurrent_external_success_local_failure | dev | SAME_PAYLOAD | CONCURRENT_FIRST_WRITE | EXTERNAL_SUCCESS_LOCAL_PERSIST_FAILURE | 1 |
| idempotency:payload_conflict_after_external_success_local_failure | validation | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_LOCAL_FAILURE | EXTERNAL_SUCCESS_LOCAL_PERSIST_FAILURE | 1 |
| idempotency:payload_conflict_after_timeout_before_acceptance | validation | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_UNCERTAIN_RESULT | TIMEOUT_BEFORE_ACCEPTANCE | 0 |
| idempotency:concurrent_timeout_after_acceptance | test | SAME_PAYLOAD | CONCURRENT_FIRST_WRITE | TIMEOUT_AFTER_ACCEPTANCE | 1 |
| idempotency:payload_conflict_after_response_loss_commit | test | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_COMMIT_RESPONSE_LOSS | RESPONSE_LOSS_AFTER_LOCAL_COMMIT | 1 |
| idempotency:payload_conflict_after_terminal_external_failure | test | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_TERMINAL_FAILURE | EXTERNAL_BUSINESS_FAILURE | 0 |
| idempotency:payload_conflict_after_timeout_after_acceptance | test | DIFFERENT_BUSINESS_PAYLOAD | SEQUENTIAL_CONFLICT_AFTER_UNCERTAIN_RESULT | TIMEOUT_AFTER_ACCEPTANCE | 1 |

### Rejected candidates

| Candidate | Taxonomy | Reason |
|---|---|---|
| candidate:same_business_payload_different_approval_metadata | EVALUATOR_UNOBSERVABLE | Production semantic hash ignores approvalId, but current benchmark driver cannot replay the same semantic payload with a newly generated approval while keeping the same logical operation without driver hardening. |
| candidate:different_key_same_business_target | GOLD_AMBIGUOUS | Current production idempotency key defines operationRequestId as logical-operation identity; no separate business contract proves two different keys for the same refund target should collapse to one logical effect. |
| candidate:timeout_before_then_success_same_key | FAULT_POINT_UNAVAILABLE | Current RecordingRefundExternalSystem uses one static simulation value per case; it cannot deterministically model first timeout-before-acceptance followed by a later accepted attempt without benchmark-driver/test-fixture hardening. |
| candidate:concurrent_same_key_different_payload | EVALUATOR_UNOBSERVABLE | Current benchmark executor only injects the conflicting payload after the initial delivery batch; it cannot establish a deterministic simultaneous P1/P2 first-write race. |
| candidate:workers_10_concurrent_retry | NOT_SEMANTICALLY_DISTINCT | Changing workers from 5 to 10 is workload intensity, not a new idempotency semantic root. |
| candidate:approval_replay_plus_idempotency | BELONGS_TO_GOVERNANCE | The primary question is approval consumption/replay, already owned by Governance; repackaging it would duplicate another benchmark. |
| candidate:refund_outbox_redelivery | UNSUPPORTED_RUNTIME | Refund execution has no Rabbit refund consumer; MQ redelivery would be a fabricated runtime path. |

## Critic rule

A candidate is not new merely because workers, retry count, seed, `operationRequestId`, `orderId`, or refund amount changed. Candidates whose primary causal question is approval consumption remain Governance; refund MQ redelivery is rejected because no refund Rabbit consumer exists.
