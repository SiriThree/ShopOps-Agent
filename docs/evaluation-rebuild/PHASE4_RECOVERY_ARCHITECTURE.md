# Phase 4 Recovery Benchmark Architecture

## Goal

Evaluate recovery as an external-consistency property:

```text
Converged = TerminalStateReached
            AND LocalStateConsistentWithExternalReality
            AND no duplicate business effect during recovery
```

This is separate from retry success and separate from Phase 3 duplicate-side-effect benchmarking.

## Unified benchmark path

Phase 4 reuses the existing framework:

```text
BenchmarkCase (RECOVERY)
→ ShopOpsBenchmarkRunner
→ RecoveryBenchmarkExecutor
→ RefundRecoveryBenchmarkExecutor
→ real ToolGateway / Approval / refund write runtime
→ production WriteOperationReconciliationService
→ independent RecordingRefundExternalSystem
→ CollectedEvidence
→ StateConvergenceEvaluator
→ EvaluationRecord
→ RecoveryMetricsAggregator
→ BenchmarkReportWriter
```

No parallel benchmark agent, write service, or recovery state machine was created.

## Production recovery path

For the flagship refund path:

```text
ToolGateway
→ Approval
→ HighRiskRefundExecuteExecutor
→ WriteOperationService.prepare
→ RefundExternalClient.execute
→ RefundExternalTransport
→ external reality

fault / ambiguity
↓
WriteOperationReconciliationService.reconcileOperation
↓
query externalReference OR operationRequestId
↓
recoverExternalSucceeded / failed / needsReconciliation / needsManualAction
↓
final WriteOperation
```

The benchmark executor only injects faults, invokes production entry points, calls the production reconciliation service, and reads facts. It does not set `WriteOperation.status`.

## Independent External Reality

The external source of truth remains `RecordingRefundExternalSystem` from Phase 3:

- does not read `WriteOperation`;
- generates its own `externalEffectId`;
- records accepted effects independently;
- reports `NOT_ACCEPTED`, `SUCCEEDED`, `FAILED`, or `DUPLICATE` from external state;
- supports `NON_IDEMPOTENT_EXTERNAL`, so a second production external call would create a second effect rather than being silently deduplicated by the fake system.

## Recovery attempt model

A recovery attempt is a real call to `WriteOperationReconciliationService.reconcileOperation(...)`. It is persisted through `recovery_attempt_count` / `last_recovery_at`. Ordinary polling is not counted.

Current Phase 4 distinctions:

- execution retry: existing write executor replay/attempt;
- reconciliation attempt: Phase 4 persisted recovery attempt;
- stale AgentTask recovery: separate JDBC worker path;
- manual review transition: terminal safety escalation, not automatic recovery.

## State evaluator

`StateConvergenceEvaluator` compares normalized enum states, not text fragments.

For the current refund model:

| External reality | Correct local state | Interpretation |
|---|---|---|
| `SUCCEEDED` | `SUCCEEDED` | externally and locally correct |
| `NOT_ACCEPTED` | `FAILED` | safe confirmed non-effect |
| `FAILED` | `FAILED` | external failure correctly reflected |
| `UNKNOWN` | `NEEDS_MANUAL_ACTION` only when Gold allows manual review | safe terminal but not automatic recovery |
| `DUPLICATE` | `NEEDS_MANUAL_ACTION` | duplicate is surfaced, never hidden as success |

`External=SUCCEEDED / Local=FAILED` is an incorrect terminal state. `External=SUCCEEDED / Local=EXECUTING` is non-terminal/stuck. A manual-review terminal is reported separately and is not silently counted as automatic convergence.

## Concurrency handling

Phase 4 discovered a real stale-snapshot race during duplicate reconciliation: one recovery thread reached `SUCCEEDED`, while another thread’s exception path tried to write `NEEDS_RECONCILIATION` from an old snapshot. Production now re-reads the current operation before recovery attempts and unresolved transitions, and `needsReconciliation(...)` refuses to move a terminal operation backwards.

This preserves the invariant:

```text
terminal state → never back to recovery intermediate
```

## Formal gate

A formal State Convergence Rate requires:

- production recovery code actually executed;
- independent external truth;
- fault injected at a real production boundary;
- benchmark did not directly repair state;
- final local state observed;
- duplicate side-effect count independently checked;
- for production-grade database claims, JDBC/MySQL runtime executed.

The current sandbox has no Maven/Docker runtime, so Phase 4 supplies executable PURE/MEMORY recovery evidence but does not publish a formal convergence rate.
