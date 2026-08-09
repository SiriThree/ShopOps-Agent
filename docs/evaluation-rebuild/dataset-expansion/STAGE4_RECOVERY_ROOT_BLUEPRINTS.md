# Stage 4 Recovery Root Blueprints

Machine source: `benchmark/v1/recovery/stage4/recovery-root-blueprints.json`.

## Review flow

```text
causal blueprint
→ fault/state/external-truth feasibility
→ critic
→ split assignment
→ case generation
```

Candidate roots proposed: **17**  
Accepted: **8**  
Rejected: **9**

## Accepted roots

| Root | Causal distinction | Split |
|---|---|---|
| `query_failure_then_not_accepted` | transient query failure, then definitive NOT_ACCEPTED | test |
| `query_failure_from_external_unknown_success` | EXTERNAL_UNKNOWN + externalReference, query failure then success | validation |
| `last_allowed_query_success` | success arrives on final permitted recovery attempt | test |
| `budget_exhausted_not_accepted` | truth is NOT_ACCEPTED but automatic query budget is exhausted | validation |
| `budget_exhausted_external_unknown_success` | initial EXTERNAL_UNKNOWN + SUCCEEDED truth, budget exhausted | test |
| `state_update_failure_from_executing` | initial crash leaves EXECUTING, then first recovery state update fails | test |
| `concurrent_reconciliation_from_executing` | concurrent recovery from EXECUTING using request-key correlation | dev |
| `concurrent_not_accepted` | concurrent recovery converges to FAILED with zero external effects | test |

All eight use existing production state transitions and existing fault/external contracts. No new production behavior was introduced.

## Rejected candidates

| Candidate | Decision | Reason |
|---|---|---|
| stale scan / old `updated_at` | REJECTED_NO_FIXTURE | no controllable clock/non-mutating stale fixture |
| ambiguous submit later reports FAILED | REJECTED_EXTERNAL_TRUTH_UNAVAILABLE | current independent external test truth cannot establish this causal sequence |
| final external UNKNOWN | REJECTED_EXTERNAL_TRUTH_UNAVAILABLE | current independent truth ledger cannot ground UNKNOWN as final reality |
| correlation completely unavailable | REJECTED_NO_FIXTURE | durable request id is NOT NULL and provider contract exposes request-key lookup |
| initial EXTERNAL_SUCCEEDED checkpoint | REJECTED_FAULT_POINT_UNAVAILABLE | no fault boundary stops at that durable checkpoint |
| initial LOCAL_CONFIRMED checkpoint | REJECTED_FAULT_POINT_UNAVAILABLE | no fault boundary stops between LOCAL_CONFIRMED and SUCCEEDED |
| deterministic version/CAS conflict after query | REJECTED_FAULT_POINT_UNAVAILABLE | existing R8 covers concurrent stale observations, but no separate deterministic CAS fixture |
| workers=10 variant | REJECTED_NOT_SEMANTICALLY_DISTINCT | workload variation only |
| extra early retry-count variant | REJECTED_NOT_SEMANTICALLY_DISTINCT | still same EARLY_SUCCESS causal region |

## Important boundary

The rejected `FAILED` and `UNKNOWN` roots are coverage gaps, not claims that production cannot handle those statuses. `WriteOperationReconciliationService` contains branches for `FAILED` and unresolved/unknown results. The current Stage 4 **dataset fixture contract** cannot independently construct those final external realities without extending test infrastructure, so they are not accepted in this dataset-engineering stage.
