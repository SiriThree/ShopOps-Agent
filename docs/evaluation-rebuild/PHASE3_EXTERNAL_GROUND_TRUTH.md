# Phase 3 External Side-Effect Ground Truth

## Why local `WriteOperation` is not ground truth

`WriteOperation.status=SUCCEEDED` is a ShopOps local claim. Using it to count external effects would be circular. Phase 3 counts external effects from an independently maintained external-system ledger.

## Boundary introduced

Production `RefundExternalClient` now delegates the external I/O boundary to `RefundExternalTransport`.

```text
HighRiskRefundExecuteExecutor
  -> RefundExternalClient            (production)
  -> RefundExternalTransport         (infrastructure boundary)
```

Normal application behavior uses `SimulatedRefundExternalTransport`. Its execute outcomes remain compatible with the existing local simulation (`success`, `failure`, `timeout_after_success`), while query behavior is now stateful instead of treating any arbitrary reference as successful; Phase 3 also adds an explicit `timeout_before_success` simulation. Reliability tests replace **only** `RefundExternalTransport` with `RecordingRefundExternalSystem`; the production executor, `WriteOperationService`, Tool Gateway, approval path, and idempotency logic remain the code under test.

## Recording external system

`RecordingRefundExternalSystem`:

- never reads `WriteOperation`, local mapper state, approval state, or benchmark expected outcome;
- assigns its own `EXT-REFUND-xxxxxx` `externalEffectId`;
- stores independent attempt and accepted-effect collections;
- binds each accepted effect to logical operation id, business target, payload hash, acceptance time, and external status;
- supports `NON_IDEMPOTENT_EXTERNAL` and `IDEMPOTENT_EXTERNAL` modes;
- records timeout-after-success as an **accepted effect with a lost response**, not as a failed/no-effect call.

This means a local crash cannot erase the evidence that the external system already accepted a refund.

## Proof mode

The main ShopOps-own-idempotency cases use:

```text
External System Mode = NON_IDEMPOTENT_EXTERNAL
```

Every accepted external call creates a new effect. Therefore if two repeated ShopOps executions reach the external boundary, the ledger exposes two effective effects. The external test system does not save ShopOps by deduplicating on its behalf.

`IDEMPOTENT_EXTERNAL` exists only as a comparison mode and must not be used to claim ShopOps itself prevented duplicates.

## Effect definition for refund

One effective refund side effect is one independently accepted external effect:

```text
externalEffectId
operationType = order.refund_execute
logicalOperationId = operationRequestId
businessTarget = orderId
payloadHash = hash(orderId | refundAmount)
externalStatus = SUCCEEDED
```

External method invocation count and effect count are separate. `timeout_before_success` produces an attempt but no effect. `timeout_after_success` produces both an attempt and one effect even though ShopOps sees `UNKNOWN`.

## Evidence source labels

- `EXTERNAL_TEST_SYSTEM`: independent in-process test external state.
- `PRODUCTION_WRITE_OPERATION`: local ShopOps state reference.
- `TOOL_LOG`: ShopOps invocation evidence.
- Future real third-party adapter evidence must use an explicit `EXTERNAL_REAL_SYSTEM` source and cannot reuse the simulated label.

## Credibility boundary

The in-process ledger can prove behavior against a deliberately non-idempotent external semantic boundary. It **does not** prove a commercial platform refund API was called. Current Phase 3 external classification is `EXTERNAL_SIMULATED` / `IN_PROCESS_TEST_DOUBLE` unless a future external-real run is executed.
