# Phase 4 Handoff — State Convergence / Recovery Benchmark

## What Phase 4 implemented

Phase 4 extends the existing ShopOpsBench framework with a real recovery experiment while preserving the Phase 0–3 runner/evidence/report contracts.

Implemented:

- `RECOVERY` execution in `ShopOpsBenchmarkRunner`;
- Phase 4 recovery case fields in the versioned `BenchmarkCase` schema;
- `RecoveryBenchmarkExecutor` / `RefundRecoveryBenchmarkExecutor`;
- `StateConvergenceEvaluator`;
- recovery raw-count aggregation/reporting;
- independent external/local state comparison;
- bounded persisted recovery attempts;
- request-correlation lookup for the refund external boundary;
- concurrent reconciliation protection;
- recovery dataset `benchmark/v1/recovery/{dev,validation,test}`;
- production/JDBC integration test code and recovery invariant tests.

## Highest-value production bug fixed

### Original condition

```text
external refund accepted
→ fault AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM
→ WriteOperation still EXECUTING
→ externalReference absent
→ original reconciliation cannot query external state
```

### Root cause

The durable `operationRequestId` was already persisted before the external call and was passed to the external transport, but the query contract only accepted `externalReference`, which is available only after a successful response is processed.

### Fix

`RefundExternalTransport` / `RefundExternalClient` now support explicit query-by-`operationRequestId`. Reconciliation uses:

```text
externalReference when available
otherwise durable operationRequestId
```

The IDs are not conflated. The adapter must actually support request-key lookup.

## Second production bug found by concurrency recovery

Two concurrent reconciliation workers could race after one worker reached `SUCCEEDED`: the stale worker’s exception path attempted `SUCCEEDED → NEEDS_RECONCILIATION`.

Fixes:

- re-read latest state after recording a recovery attempt;
- re-read latest state in exception/unresolved paths;
- `needsReconciliation(...)` never moves a terminal operation backwards;
- memory recovery-attempt accounting does not mutate a terminal operation.

The two-worker PURE/MEMORY scenario now ends `SUCCEEDED` with one external effect.

## State-machine change

Production `WriteOperationStatus` now includes `NEEDS_MANUAL_ACTION` as a stable safe terminal state. It is used only when bounded recovery cannot establish/commit a safe automatic result. It is reported separately from automatic convergence.

## Schema support

Migration `V23__phase4_write_recovery.sql` adds:

- `recovery_attempt_count`;
- `last_recovery_at`;
- a reconciliation lookup index.

This is production reliability state, not benchmark-only storage.

## External ground truth

Phase 4 continues to use the independent `RecordingRefundExternalSystem` from Phase 3 in deterministic tests. It does not read local `WriteOperation`, generates its own effect IDs, and can run as a non-idempotent external system.

Limitation: it is an in-process test external system, not a commercial refund API. Query-by-request-id support must be verified when a real provider connector is implemented.

## Actually verified in this environment

PURE/MEMORY production recovery harness:

```text
R1 external success + local failure       PASS
R2 timeout before acceptance              PASS
R3 timeout after acceptance               PASS
R5 temporary reconciliation failure       PASS
R6 recovery budget exhausted              PASS as expected manual terminal
R8 concurrent duplicate reconciliation    PASS
```

Observed across those six executed semantic scenarios:

```text
Terminal reached           6
State correct              5
Manual review              1
Recovery attempts         10
Duplicate effects          0
```

These are not formal benchmark rates.

## Runtime verification gap

Current environment still lacks Maven/Docker. Therefore:

```text
Spring/JUnit                    NOT RUN
JdbcRefundRecoveryIntegration  NOT RUN
MySQL status/CAS recovery       NOT RUNTIME VERIFIED
RabbitMQ refund recovery        NOT SUPPORTED (no refund Rabbit consumer)
Commercial external API         UNAVAILABLE
Formal State Convergence Rate   NOT AVAILABLE
```

Memory-mode results do not substitute for JDBC transaction/CAS verification.

## AgentTask recovery audit

The existing JDBC agent path has a real five-minute worker lease and stale `RUNNING/QUEUED` redispatch path. It is separate from refund WriteOperation recovery and was not included in Phase 4 refund convergence results. In-memory AgentTask recovery remains unsupported.

One current limitation worth future work: `TaskErrorType` exposes retryable/max-attempt metadata, but `JdbcAgentTaskExecutionWorker.failTask(...)` does not currently apply those budgets; classified execution errors go directly to terminal failure/manual action.

## Production changes classification

| Change | Classification | Why |
|---|---|---|
| query refund by durable request correlation | RECOVERY BUG FIX | close external-success/missing-reference crash window |
| `NEEDS_MANUAL_ACTION` write terminal | RECOVERY BUG FIX / SAFETY | bounded safe escalation |
| recovery attempt columns | SCHEMA SUPPORT / OBSERVABILITY | bounded recovery and auditable attempts |
| additional reconciliation fault points | TESTABILITY / OBSERVABILITY | inject failures at real production boundaries |
| terminal-state stale-race guards | RECOVERY BUG FIX | prevent concurrent recovery from regressing success |
| executor blocking recoverable/manual/failed re-execution | SAFETY | recovery must query reality, not repeat refund |

No natural-language routing, planner behavior, approval policy, or benchmark-specific production branch was added.

## Phase 5 entry

Next phase: **Execution Governance Benchmark**.

Reuse directly:

- `ShopOpsBenchmarkRunner` and versioned `BenchmarkCase`;
- Phase 1 identity propagation audit and trusted `ToolInvokeContext`;
- `DefaultToolGatewayService` governance checks;
- real `JdbcApprovalRequestService` / approval binding;
- `WriteOperationService` / refund execution path;
- independent external ground truth;
- Phase 3/4 fault framework;
- `EvaluationRecord` / EvidenceRef / report infrastructure.

Phase 5 should measure both illegal and legal requests: unauthorized block rate **and** false reject rate, plus unauthorized write, approval bypass, and cross-tenant violation counts. Do not average governance with task/idempotency/recovery into a single score.

## Phase 3 regression re-check

After the Phase 4 recovery fixes, a deterministic production-code harness re-ran sequential retry, five-way concurrent retry, same-key/different-payload, timeout-after-acceptance replay, and external-success/local-failure replay. All five preserved one effective external effect; payload conflict remained rejected, and ambiguous/recoverable writes remained blocked from direct re-execution.
