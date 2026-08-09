# Stage 4 Recovery Dataset Card

## Identity

```text
Benchmark          RECOVERY
Dataset version    1.4.0-stage4-recovery-candidate
Gold version       shopopsbench-gold-v1.4-recovery-stage4
Status             EXPANSION_CANDIDATE
Formal run         false
```

## Size

```text
Total cases        21
Causal roots       15

dev                7 cases / 4 roots
validation         8 cases / 5 roots
test               6 cases / 6 roots
test-exclusive     6 roots
```

## External reality

```text
SUCCEEDED          16 cases / 11 roots
NOT_ACCEPTED        5 cases / 4 roots
FAILED              0
UNKNOWN             0
```

## Initial state

```text
EXECUTING           8 cases / 5 roots
EXTERNAL_UNKNOWN   13 cases / 10 roots
```

`EXTERNAL_SUCCEEDED` and `LOCAL_CONFIRMED` are valid production states but are not dataset roots because no current deterministic fault point can stop the write chain at those checkpoints without direct state mutation.

## Budget

```text
EARLY_SUCCESS           11 roots
LAST_ALLOWED_SUCCESS     1 root
BUDGET_EXHAUSTED         3 roots
```

## Concurrency

```text
Concurrent cases         4
Concurrent causal roots  3
```

Changing worker count alone does not create a new root.

## Review

```text
MODEL_REVIEWED                    21
Evidence-backed HUMAN_REVIEWED     0
Historical humanReviewed=true     13
New humanReviewed=true             0
```

Historical `humanReviewed=true` remains provenance-uncertain metadata.

## Gold

All 21 dedicated cases use `FAULT_CONTRACT_DERIVED`. External truth is defined independently through the `RecordingRefundExternalSystem` contract; Gold is not inferred from local `WriteOperation.status`.

Every case explicitly carries:

```text
sideEffectExpectation.expectedLogicalSideEffects
expectedEffectiveSideEffects
sideEffectExpectation.constraints.duplicateSideEffectsAllowed = 0
```

## Held-out discipline

The six Stage 4 candidate test roots were not executed against Tool Gateway/recovery runtime during this stage. Only schema, causal-root, Gold, fixture-contract and source-isolation checks are permitted.
