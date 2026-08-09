# Phase 3 Side-Effect Idempotency Architecture

## Benchmark objective

The benchmark counts business effects, not calls:

```text
Logical Write Request
  -> repeated Delivery Attempts
  -> repeated production execution attempts
  -> External Attempts
  -> Independent External Effects
  -> duplicate/missing effect evaluation
```

The metric contract remains:

```text
duplicateSideEffects = max(actualEffectiveSideEffects - expectedLogicalSideEffects, 0)
missingSideEffects   = max(expectedLogicalSideEffects - actualEffectiveSideEffects, 0)
Duplicate Side Effect Rate = duplicateSideEffects / expectedLogicalSideEffects
```

Zero duplicates is not a PASS if a required effect is missing.

## Unified runner

Phase 3 does not create a second framework. `ShopOpsBenchmarkRunner` now dispatches `IDEMPOTENCY` cases to an `IdempotencyBenchmarkExecutor` while retaining the Phase 1/2 TASK path.

```text
BenchmarkCase(IDEMPOTENCY)
  -> ShopOpsBenchmarkRunner
  -> RefundIdempotencyBenchmarkExecutor
  -> ToolGatewayService
  -> real ApprovalRequestService
  -> HighRiskRefundExecuteExecutor
  -> WriteOperationService
  -> RefundExternalClient
  -> RecordingRefundExternalSystem
  -> SideEffectIdempotencyEvaluator
  -> EvaluationRecord
  -> IdempotencyMetricsAggregator
  -> JSON / Markdown report
```

No runner-side request deduplication is performed. Every configured post-approval delivery calls `ToolGatewayService.invoke`.

## Execution levels

- `TOOL_GATEWAY`: flagship benchmark entry; exercises permission/risk/approval/tool/write path.
- `WRITE_OPERATION`: narrow reliability/unit harness may directly exercise write service or executor; results must be labeled accordingly.
- `AGENT`: currently not applicable because NL planning does not reach refund writes.
- `EXTERNAL_ADAPTER`: external-system-only contract tests; never reported as ShopOps write idempotency.

## Attempt model

Phase 3 evidence can represent:

- `logicalWriteRequests`: one business logical request keyed by operationRequestId / idempotency key.
- `deliveryAttempts`: benchmark-configured repeated deliveries after approval.
- `executionAttempts`: deliveries that reached the production write execution decision (derived from Tool Gateway result in deterministic mode).
- `toolAttempts`: real ToolCallLog entries; includes approval negotiation and write attempts.
- `externalAttempts`: independent external transport attempts.
- `externalEffects`: independently accepted external effects.
- `idempotencyDecisions`: replay, in-progress block, external-unknown block, payload mismatch.
- `writeOperationTransitions`: compact local write snapshots.

The current deterministic delivery count is **not Rabbit metadata**. Real queue redelivery evidence remains unavailable.

## Payload conflict policy

Same key + different semantic payload is rejected. The production executor converts `IdempotencyConflictException` to `IDEMPOTENCY_PAYLOAD_MISMATCH`. The evaluator requires the expected conflict to be observed when the case requests `payloadConflictBehavior=REJECT`.

## Dataset

`benchmark/v1/idempotency/{dev,validation,test}/cases.json` contains 14 hand-reviewed semantic cases across baseline, retries, concurrency, payload conflict, timeout ambiguity, local failure after external success, response loss after local confirm, and external-idempotency comparison. Delivery counts are attributes of cases and are never reported as unique benchmark cases.

## Formal gate

A formal duplicate-effect result requires all of:

1. production write service executed;
2. production write executor executed;
3. production idempotency boundary executed;
4. independent external ground truth enabled;
5. repeated attempts actually reach the production boundary;
6. no harness-side deduplication;
7. for claims about database/concurrency semantics, a real JDBC/MySQL path run.

A PURE javac harness can validate code and expose deterministic write semantics but does not replace Spring/JDBC integration verification.
