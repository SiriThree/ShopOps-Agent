# Phase 4 State Machine Audit

## Scope

This audit was performed against the Phase 3-derived repository source, not the previous handoff narrative. The primary recovery benchmark scope is `order.refund_execute` / `WriteOperation`; `AgentTask` recovery is audited separately and is not merged into the refund convergence metric.

## WriteOperation state machine

Production source: `reliability/domain/WriteOperationStatus.java`, transitions executed by `WriteOperationService` and `WriteOperationReconciliationService`.

| State | Terminal? | Typical allowed next states | Retry / recovery meaning | External query needed? | Safe to re-execute refund? |
|---|---:|---|---|---:|---:|
| `CREATED` | No | `WAITING_APPROVAL`, `APPROVED`, `FAILED` | request recorded | No | No: continue workflow |
| `WAITING_APPROVAL` | No | `APPROVED`, `FAILED` | waiting for human decision | No | No |
| `APPROVED` | No | `EXECUTING`, `FAILED` | approved, not yet executing | No | Only through normal executor transition |
| `EXECUTING` | No | `EXTERNAL_UNKNOWN`, `EXTERNAL_SUCCEEDED`, `FAILED`, `NEEDS_RECONCILIATION`, `NEEDS_MANUAL_ACTION` | external call may be in-flight or its response may have been lost | **Yes when stale/ambiguous** | **No** until external reality is known |
| `EXTERNAL_UNKNOWN` | No | `EXTERNAL_SUCCEEDED`, `FAILED`, `NEEDS_RECONCILIATION`, `NEEDS_MANUAL_ACTION` | response ambiguous | Yes | No |
| `EXTERNAL_SUCCEEDED` | No | `LOCAL_CONFIRMED`, `NEEDS_RECONCILIATION`, `NEEDS_MANUAL_ACTION` | external success known, local workflow incomplete | No external re-execution; may query for verification | No |
| `LOCAL_CONFIRMED` | No | `SUCCEEDED`, `NEEDS_RECONCILIATION`, `NEEDS_MANUAL_ACTION` | local post-external confirmation completed | No | No |
| `NEEDS_RECONCILIATION` | No | `EXTERNAL_SUCCEEDED`, `LOCAL_CONFIRMED`, `FAILED`, `NEEDS_MANUAL_ACTION` | bounded recovery required | Yes | No |
| `SUCCEEDED` | Yes | none | stable success | No | No |
| `FAILED` | Yes | none | confirmed failure / not accepted | No | No; intentional business retry needs a new logical request |
| `NEEDS_MANUAL_ACTION` | Yes, safe terminal | none | bounded automatic recovery exhausted or unsafe ambiguity | External truth may still exist, but automatic mutation stops | No |

`WriteOperationStatus.terminal(...)` therefore means `SUCCEEDED`, `FAILED`, or `NEEDS_MANUAL_ACTION`. A terminal state alone is **not** convergence: the benchmark independently compares it with external truth.

### Production invariants enforced in Phase 4

- A terminal state cannot transition back to `EXECUTING` or `NEEDS_RECONCILIATION`.
- A recovery worker that observes another concurrent worker already reaching `SUCCEEDED` returns that terminal result instead of applying a stale recovery transition.
- `HighRiskRefundExecuteExecutor` blocks direct external re-execution from `EXTERNAL_UNKNOWN`, `EXTERNAL_SUCCEEDED`, `LOCAL_CONFIRMED`, `NEEDS_RECONCILIATION`, `NEEDS_MANUAL_ACTION`, and `FAILED`.
- `SUCCEEDED` is reached by the production local-confirm chain only after external success has been observed.
- `NEEDS_MANUAL_ACTION` never silently returns to automatic execution.

## Refund recovery correlation state

The durable logical correlation identity is `operation_request_id` / `WriteOperation.operationRequestId`. It exists before the external call and is passed into `RefundExternalTransport.execute(...)`. Phase 4 does **not** set `externalReference = operationRequestId`; instead the external contract now exposes an explicit `queryByOperationRequestId(...)` capability.

The reconciliation lookup order is:

1. if a durable `externalReference` exists, query by external reference;
2. otherwise query by the pre-call `operationRequestId` correlation identity;
3. if the adapter cannot establish external reality, remain recoverable until the bounded recovery budget is exhausted;
4. after budget exhaustion, enter `NEEDS_MANUAL_ACTION` rather than re-executing the refund.

## Recovery budget

`WriteOperationReconciliationService` has a bounded production recovery budget (`shopops.reliability.reconciliation-max-attempts`, default `3`). `WriteOperation` persists:

- `recovery_attempt_count`;
- `last_recovery_at`.

Migration: `V23__phase4_write_recovery.sql`.

No unbounded retry loop was added.

## AgentTask state machine (separate metric domain)

Production enum `AgentTaskStatus` contains:

`PENDING → QUEUED/RUNNING`, then `RUNNING`, `WAITING_APPROVAL`, `RETRYING`, and terminal `SUCCEEDED`, `FAILED`, `CANCELLED`, `NEEDS_MANUAL_ACTION`; `CANCEL_REQUESTED` is an intermediate cancellation state.

JDBC async recovery facts:

- `JdbcAgentTaskExecutionWorker` acquires a five-minute worker lease (`LEASE_DURATION = 5 minutes`).
- `AgentTaskMapper.listStaleInFlight(...)` treats old `QUEUED` tasks and expired `RUNNING` leases as stale.
- `JdbcAgentTaskService.requeueStaleTasks(...)` can move stale `RUNNING → QUEUED` with a compare-and-set status update and redispatch it.
- `InMemoryAgentTaskService.requeueStaleTasks(...)` currently returns an empty recovery result; memory mode does not model this stale worker recovery.
- `TaskErrorType` declares retryability/max-attempt metadata, but the current `JdbcAgentTaskExecutionWorker.failTask(...)` does not consume those `retryable/maxAttempts` fields; it transitions classified failures directly to `FAILED` or `NEEDS_MANUAL_ACTION`. This is a real current limitation, not treated as a completed retry framework.

Phase 4 formal recovery work therefore reports **WriteOperation recovery**. AgentTask stale recovery remains an independently auditable path and must not be folded into the refund convergence rate.
