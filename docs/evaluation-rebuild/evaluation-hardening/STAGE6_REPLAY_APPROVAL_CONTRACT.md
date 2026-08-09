# Stage 6 Replay Approval Contract

## Fresh approval per intended replay

Each intended high-risk replay receives a distinct approval ID. The approval is not cloned and its status is never mutated directly by benchmark code.

The helper is `FreshReplayApprovalFactory`.

Each approval is created through:

`ToolGateway -> APPROVAL_REQUIRED -> ApprovalRequestService.approve(...)`

The production approval lifecycle remains unchanged.

## Logical operation invariants

Fresh approval is governance setup metadata. It does not create a new logical operation.

Within a same-payload replay:

- `operationRequestId`: unchanged;
- idempotency key: unchanged;
- business payload: unchanged;
- business target: unchanged;
- `approvalId`: different per attempt.

Approval creation count is not:

- semantic scenario count;
- logical operation count;
- intended replay count.

## Canonical semantic hash audit

Current production `WriteOperationService.canonicalSemanticInput(...)` explicitly removes `approvalId` before hashing.

Current gateway canonical input used for approval binding also removes `approvalId`.

For the refund path, approval ID is carried in `ToolInvokeContext`, while the business input remains unchanged.

Therefore a fresh approval for the same K/P does not change the logical-operation semantic payload.

## Conflict counterexample

For payload conflict, a new approval is created for P2 itself.

The intended chain is:

`K + P1 + approval A -> write operation`

then

`K + P2 + approval B -> approval valid -> WriteOperationService -> IDEMPOTENCY_PAYLOAD_MISMATCH`

This prevents approval-payload mismatch from masking the actual idempotency conflict.

## Runtime verification status

Source implementation and isolated compilation are verified. Spring/JUnit contract tests that exercise the real in-memory gateway lifecycle are present but NOT RUN because Maven/Maven Wrapper is unavailable.
