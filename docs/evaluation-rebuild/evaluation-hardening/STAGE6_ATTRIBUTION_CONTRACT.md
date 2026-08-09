# Stage 6 Idempotency Attribution Contract

## Principle

`duplicate side effects = 0` is application-idempotency evidence only if all intended legal attempts have passed non-idempotency governance prerequisites and reached the `WriteOperationService` idempotency boundary.

The contract is:

`valid identity -> valid authorization -> valid schema -> valid business scope -> fresh valid approval -> write executor -> WriteOperationService -> idempotency decision -> optional external attempt`

## Pre-idempotency block is not idempotency success

The benchmark distinguishes:

- `ATTRIBUTION_INVALID_AUTHORIZATION_BLOCK`
- `ATTRIBUTION_INVALID_SCHEMA_BLOCK`
- `ATTRIBUTION_INVALID_SCOPE_BLOCK`
- `ATTRIBUTION_INVALID_APPROVAL_BLOCK`
- `ATTRIBUTION_INVALID_PRE_IDEMPOTENCY_BLOCK`

from boundary decisions such as:

- `IDEMPOTENCY_REPLAY_DEDUPED`
- `IDEMPOTENCY_PAYLOAD_CONFLICT`
- `IDEMPOTENCY_CONCURRENT_WINNER`
- `IDEMPOTENCY_BOUNDARY_REACHED`

A pre-boundary block invalidates application-idempotency attribution.

## Attribution eligibility

`IdempotencyAttributionEligibility` requires:

1. first legal operation reaches the idempotency boundary;
2. every intended replay reaches the idempotency boundary;
3. no intended operation/replay is blocked before that boundary.

If any condition is false, `SideEffectIdempotencyEvaluator` records `IDEMPOTENCY_ATTRIBUTION_INVALID` when Stage 6 attribution evidence is required.

## Missing and duplicate effects

The external side-effect contract remains:

`duplicateSideEffects = max(actualEffectiveSideEffects - expectedEffectiveSideEffects, 0)`

`missingSideEffects = max(expectedEffectiveSideEffects - actualEffectiveSideEffects, 0)`

Therefore rejecting every attempt cannot produce a perfect idempotency score: if one effect is expected and zero occurs, missing effects equals one and the case fails.

## External ground truth

`RecordingRefundExternalSystem` remains independent of `WriteOperation` persistence.

It records separately:

- external attempt;
- effective external side effect.

A provider invocation that rejects the business operation can increase external attempts without increasing effective effects.

## Formal attribution eligibility

Formal Idempotency eligibility now additionally requires:

- `ATTRIBUTION_ISOLATION_VERIFIED`
- `REPLAY_REACHED_IDEMPOTENCY_BOUNDARY`
- `MISSING_EFFECT_MEASURABLE`

These gates are additive to the existing Spring/JDBC/Tool Gateway/WriteOperation/external-ground-truth/non-idempotent-external requirements.

In the current environment these runtime gates are not promoted to verified because Spring/JUnit and JDBC Formal runs were not executed.
