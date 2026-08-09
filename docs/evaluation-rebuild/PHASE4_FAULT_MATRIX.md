# Phase 4 Recovery Fault Matrix

`PASS` below means the listed **PURE/MEMORY production-recovery harness** scenario was actually executed successfully. It does not mean JDBC/Spring formal verification passed.

| Fault / Case | Injection point / mechanism | External reality | Expected recovery | Expected terminal | Expected effects | Implemented | PURE/MEMORY verified | Spring/JDBC verified |
|---|---|---|---|---|---:|---:|---|---|
| R1 External success + local failure | `AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM` | `SUCCEEDED` | query by durable `operationRequestId`, resume local confirmation | `SUCCEEDED` | 1 | YES | **PASS** | NOT RUN |
| R2 Timeout before external acceptance | external simulation `timeout_before_success` | `NOT_ACCEPTED` | query correlation key, confirm no acceptance | `FAILED` | 0 | YES | **PASS** | NOT RUN |
| R3 Timeout after external acceptance | external simulation `timeout_after_success` | `SUCCEEDED` | query external reference/request correlation | `SUCCEEDED` | 1 | YES | **PASS** | NOT RUN |
| R4 Local confirm/update failure | recovery checkpoints support `BEFORE_RECOVERY_STATE_UPDATE`; external-success local-confirm chain is resumable | `SUCCEEDED` | retry reconciliation without re-executing external call | `SUCCEEDED` | 1 | PARTIAL | test code implemented; held-out test not executed | NOT RUN |
| R5 Reconciliation query temporary failure | `BEFORE_RECONCILIATION_QUERY`, first recovery attempt | `SUCCEEDED` | remain `NEEDS_RECONCILIATION`, retry boundedly | `SUCCEEDED` | 1 | YES | **PASS** (2 attempts) | NOT RUN |
| R6 Recovery budget exhausted | `BEFORE_RECONCILIATION_QUERY`, attempts 1..3 | `SUCCEEDED` but unavailable to local recovery during budget | bounded retry then safe escalation | `NEEDS_MANUAL_ACTION` | 1 | YES | **PASS as expected safe terminal**, not converged | NOT RUN |
| R7 Stale intermediate WriteOperation | `findForReconciliation` uses status + updated-at cutoff | depends on external | scheduled reconciliation | state-dependent | no duplicate | PARTIAL | production query exists; scheduler/time advancement not runtime tested | NOT RUN |
| R8 Duplicate reconciliation | two concurrent production reconciliation calls | `SUCCEEDED` | one or both observe same external truth; terminal state cannot regress | `SUCCEEDED` | 1 | YES | **PASS** (2 recovery attempts, 1 effect) | NOT RUN |
| AgentTask stale worker recovery | expired JDBC lease / stale queued task | N/A | `RUNNING → QUEUED`, redispatch | task-specific | N/A | existing production path | NOT RUN | NOT RUN |
| Refund Rabbit worker kill/redelivery | no persisted refund Rabbit consumer exists | N/A | N/A | N/A | N/A | **NOT SUPPORTED** | NOT SUPPORTED | NOT SUPPORTED |

## Important execution-level distinctions

- R1/R2/R3/R5/R6/R8 were executed by a Java 21 harness against actual production `HighRiskRefundExecuteExecutor`, `WriteOperationService`, `RefundExternalClient`, and `WriteOperationReconciliationService`, using memory persistence and the independent test external system.
- The harness did not directly set production states.
- Phase 4 dedicated `test` split cases remain held out and were not executed as development evidence.
- JDBC/MySQL and Spring ToolGateway tests exist but are `NOT RUN` in the current environment.
