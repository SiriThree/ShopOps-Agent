# Stage 4 Recovery Causal Matrix

Machine source: `benchmark/v1/recovery/stage4/recovery-causal-matrix.json`.

| Root | Initial Local | Fault / causal family | External Reality | Recovery Evidence | Budget | Expected Outcome | Split |
|---|---|---|---|---|---|---|---|
| `recovery:duplicate_reconciliation` | EXTERNAL_UNKNOWN | R8_CONCURRENT_RECONCILIATION | SUCCEEDED | externalReference | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | validation |
| `recovery:external_success_local_failure` | EXECUTING | R1_EXTERNAL_SUCCESS_LOCAL_FAILURE | SUCCEEDED | operationRequestId | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | dev |
| `recovery:reconciliation_temporary_failure` | EXECUTING | R5_RECONCILIATION_QUERY_TEMPORARY_FAILURE | SUCCEEDED | operationRequestId | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | validation |
| `recovery:recovery_budget_exhausted` | EXECUTING | R6_RECOVERY_BUDGET_EXHAUSTED | SUCCEEDED | operationRequestId | BUDGET_EXHAUSTED | MANUAL_REVIEW_REQUIRED / NEEDS_MANUAL_ACTION | validation |
| `recovery:recovery_state_update_failure` | EXTERNAL_UNKNOWN | R4_RECOVERY_STATE_UPDATE_FAILURE | SUCCEEDED | externalReference | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | test |
| `recovery:stage4:budget_exhausted_external_unknown_success` | EXTERNAL_UNKNOWN | BUDGET_INITIAL_STATE | SUCCEEDED | response-lost reference exists but all reconciliation queries are faulted | BUDGET_EXHAUSTED | MANUAL_REVIEW_REQUIRED / NEEDS_MANUAL_ACTION | test |
| `recovery:stage4:budget_exhausted_not_accepted` | EXTERNAL_UNKNOWN | BUDGET_EXTERNAL_STATE | NOT_ACCEPTED | external truth exists but every local query attempt is faulted | BUDGET_EXHAUSTED | MANUAL_REVIEW_REQUIRED / NEEDS_MANUAL_ACTION | validation |
| `recovery:stage4:concurrent_not_accepted` | EXTERNAL_UNKNOWN | CONCURRENCY_EXTERNAL_STATE | NOT_ACCEPTED | two reconciliation workers race while external system definitively reports NOT_ACCEPTED | EARLY_SUCCESS | AUTO_CONVERGED / FAILED | test |
| `recovery:stage4:concurrent_reconciliation_from_executing` | EXECUTING | CONCURRENCY_INITIAL_STATE | SUCCEEDED | two reconciliation workers race from the same unresolved EXECUTING logical write | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | dev |
| `recovery:stage4:last_allowed_query_success` | EXTERNAL_UNKNOWN | BUDGET_BOUNDARY | SUCCEEDED | first query fails; second and last allowed attempt confirms SUCCEEDED | LAST_ALLOWED_SUCCESS | AUTO_CONVERGED / SUCCEEDED | test |
| `recovery:stage4:query_failure_from_external_unknown_success` | EXTERNAL_UNKNOWN | QUERY_FAILURE_INITIAL_STATE | SUCCEEDED | response-lost external reference exists; first query fails then succeeds | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | validation |
| `recovery:stage4:query_failure_then_not_accepted` | EXTERNAL_UNKNOWN | QUERY_FAILURE_THEN_EXTERNAL_FACT | NOT_ACCEPTED | query fails once, then external query confirms NOT_ACCEPTED | EARLY_SUCCESS | AUTO_CONVERGED / FAILED | test |
| `recovery:stage4:state_update_failure_from_executing` | EXECUTING | STATE_UPDATE_INITIAL_STATE | SUCCEEDED | external accepted; initial local confirm fails; first recovery state update also faults | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | test |
| `recovery:timeout_after_external_acceptance` | EXTERNAL_UNKNOWN | R3_TIMEOUT_AFTER_ACCEPTANCE | SUCCEEDED | externalReference | EARLY_SUCCESS | AUTO_CONVERGED / SUCCEEDED | dev |
| `recovery:timeout_before_external_acceptance` | EXTERNAL_UNKNOWN | R2_TIMEOUT_BEFORE_ACCEPTANCE | NOT_ACCEPTED | operationRequestId | EARLY_SUCCESS | AUTO_CONVERGED / FAILED | dev |

## Distribution

```text
Cases / causal roots = 21 / 15

Initial local:
EXECUTING         8 cases / 5 roots
EXTERNAL_UNKNOWN 13 cases / 10 roots

External truth:
SUCCEEDED         16 cases / 11 roots
NOT_ACCEPTED       5 cases / 4 roots
FAILED             0
UNKNOWN            0

Budget regions:
EARLY_SUCCESS          11 roots
LAST_ALLOWED_SUCCESS    1 root
BUDGET_EXHAUSTED        3 roots

Outcome classes:
AUTO_CONVERGED         12 roots
MANUAL_REVIEW_REQUIRED  3 roots

Concurrency:
4 cases / 3 causal roots
```

Workers are not counted as semantic roots. The two historical R8 cases remain one root.
