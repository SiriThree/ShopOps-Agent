# Stage 5 Idempotency Workload Contract

## Four different units

```text
Semantic Scenario
!= Logical Operation
!= Repeated Request / Execution Attempt
!= External Attempt
```

A semantic scenario describes *why* repeated execution occurs and what side-effect semantics are expected. A workload profile expands a scenario into many independent logical operations. Attempts belong to a logical operation; they are not benchmark cases. External attempts are observed independently and may differ from request attempts.

## Metric denominator

The formal denominator is `logicalOperationsEvaluated`, not request-attempt count. Every formal report must retain:

- semantic scenarios;
- logical operations;
- repeated request attempts;
- external attempts;
- expected effective side effects;
- actual effective side effects;
- duplicate side effects;
- missing side effects.

`duplicateSideEffects = max(actualEffectiveSideEffects - expectedLogicalSideEffects, 0)`. Missing effects are also failures.

## External truth

Formal evidence requires `NON_IDEMPOTENT_EXTERNAL` and the independent `RecordingRefundExternalSystem` ledger. Provider idempotency must not mask an application duplicate.

## Approval boundary — formal eligibility blocker

**IDEMPOTENCY_BENCHMARK_DRIVER_GAP:** the current `RefundIdempotencyBenchmarkExecutor` creates one approval and reuses that consumed `approvalId` across repeated deliveries. Since Phase 5 approval consumption only accepts an `APPROVED` request once, a replay may be stopped at Approval before `WriteOperationService`. That is not valid evidence that application idempotency prevented a duplicate.

Before a Formal workload run, every legal replay intended to exercise the WriteOperation boundary must obtain a fresh approval bound to the same canonical business payload, or the benchmark must explicitly execute at a lower scoped boundary. Until then, the workload below is **DESIGNED, NOT FORMAL-RUNTIME-READY**.

## Planned profiles

| Profile | Semantic scenarios | Logical operations | Planned attempts | Held-out metric ops |
|---|---|---|---|---|
| SMOKE | 4 | 8 | 16 | 0 |
| INTEGRATION | 12 | 48 | 124 | 0 |
| FORMAL | 4 | 260 | 700 | 240 |

FORMAL contains **240 held-out metric logical operations** plus **20 development controls** = **260 total logical operations**, with **700 planned request/execution attempts**. The 700 attempts are not 700 benchmark cases.
