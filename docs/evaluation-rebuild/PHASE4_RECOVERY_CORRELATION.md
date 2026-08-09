# Phase 4 Recovery Correlation

## Problem reproduced from Phase 3

The high-value crash window is:

```text
WriteOperation = EXECUTING
→ RefundExternalTransport.execute(operationRequestId, ...)
→ external platform accepts refund
→ externalEffectId exists
→ fault AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM
→ local process exits before externalReference is persisted
→ local WriteOperation remains EXECUTING
```

Before Phase 4, `WriteOperationReconciliationService` only queried `RefundExternalClient.query(externalReference)`. When the response-processing path crashed before `externalReference` reached local storage, reconciliation had no key with which to ask the external system what happened.

## Chosen correlation strategy

### Durable pre-call identity

The repository already had a durable `operationRequestId`:

- persisted in `write_operation.operation_request_id` before external execution;
- included in the idempotency/write-operation domain;
- passed to `RefundExternalTransport.execute(operationRequestId, orderId, amount, simulation)`.

Phase 4 therefore uses `operationRequestId` as a **correlation/request lookup key**, not as an invented external effect ID.

### External contract change

`RefundExternalTransport` now supports:

```text
query(externalReference)
queryByOperationRequestId(operationRequestId)
```

`RefundExternalClient` exposes the same distinction.

Reconciliation behavior:

```text
externalReference present
    → query(externalReference)

externalReference absent
    → queryByOperationRequestId(operationRequestId)
```

The benchmark never assigns a fake external reference and never updates a production status itself.

## Why this closes the crash window

`operationRequestId` exists **before** the external call, so a process crash after external acceptance cannot erase the only local correlation identity. If the external adapter supports request-key lookup, recovery can discover the previously accepted operation and continue:

```text
EXECUTING
→ external query by operationRequestId = SUCCEEDED
→ EXTERNAL_SUCCEEDED
→ LOCAL_CONFIRMED
→ SUCCEEDED
```

No second refund execution is required.

## Same key / different payload

Phase 3 semantic input binding remains in force. `operationRequestId` participates in the logical write identity, while execution-only metadata such as `approvalId` is excluded from the semantic payload hash. A repeated idempotency key with a different business payload is rejected as a payload mismatch instead of being treated as a replay success.

## External ground-truth capability honesty

`RecordingRefundExternalSystem` supports request-key lookup because it is an in-process external test system specifically modeling an API with durable request correlation. `SimulatedRefundExternalTransport` now models the same protocol.

This does **not** prove that an eventual commercial platform API supports query-by-request-id. A real connector must map this contract to one of:

- provider request/idempotency key lookup;
- merchant request number lookup;
- provider-side operation search by business request key;
- another documented durable correlation mechanism.

If a real provider offers only a response-time external ID and no request-key lookup, this design gap reappears and a durable provider-specific pending-call/correlation strategy would be required.

## Trade-off analysis

| Option | Duplicate prevention | Crash-window recovery | External compatibility | Schema impact | Decision |
|---|---|---|---|---|---|
| Pre-generated fake external ID | weak unless provider accepts it | good | often unrealistic | medium | rejected |
| `operationRequestId` as lookup/idempotency correlation key | strong with provider support | strong because durable pre-call | requires provider request-key lookup | low | **selected** |
| Query by order/refund business key only | can be ambiguous across retries/partial refunds | medium | commonly possible but not always unique | low | fallback only, not implemented |
| Separate durable pending-external-call record | strong | strong | most general | higher schema/architecture cost | deferred until a provider requires it |

## Failure behavior

When external status lookup is temporarily unavailable, the operation is moved to/kept in `NEEDS_RECONCILIATION` and the recovery attempt is recorded. After the bounded recovery budget, production moves to `NEEDS_MANUAL_ACTION`. It does **not** solve uncertainty by blindly calling `execute` again.
