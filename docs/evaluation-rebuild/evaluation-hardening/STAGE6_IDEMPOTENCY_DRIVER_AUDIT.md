# Stage 6 Idempotency Driver Audit

## Scope

Stage 6 audits and hardens only ShopOpsBench Idempotency benchmark/test infrastructure. Production `src/main` is unchanged. Stage 5 dataset semantics remain 22 dedicated cases / 16 semantic scenarios / 4 test-exclusive scenarios.

## Root cause

The Stage 5 `RefundIdempotencyBenchmarkExecutor` created one approval before the delivery loop and reused that approval for every delivery.

The production gateway consumes approval state with one-shot execution semantics:

`APPROVED -> EXECUTING -> EXECUTED / EXECUTION_FAILED`.

Therefore a replay that reused the already-consumed approval could be rejected at the approval layer before `HighRiskRefundExecuteExecutor` called `WriteOperationService.prepare(...)`.

A zero-duplicate result from that path is Governance evidence ("consumed approval cannot be replayed"), not application-idempotency evidence.

## Old driver path

1. Create approval A.
2. Attempt 1 uses A and reaches the write path.
3. Approval A becomes `EXECUTED` or `EXECUTION_FAILED`.
4. Attempt 2 reuses A.
5. Gateway checks approval binding/status.
6. Replay can stop with `APPROVAL_NOT_APPROVED` / `APPROVAL_EXECUTION_CONFLICT`.
7. `WriteOperationService` may never see the replay.

## Idempotency decision boundary

The benchmark attribution boundary is the production `WriteOperationService` path reached from `HighRiskRefundExecuteExecutor.execute(...)`.

The relevant decision operations are:

- prepare/find-or-create by idempotency key;
- canonical semantic input hash comparison;
- existing operation status decision;
- concurrent winner/readback behavior.

`ToolGatewayService` is necessary governance plumbing, but is not itself the idempotency decision boundary.

## Stage 6 driver behavior

For each intended execution attempt, the benchmark now creates a fresh approval using the real gateway approval-request path and `ApprovalRequestService.approve(...)`.

Sequential replay:

- Attempt 1 -> approval A -> same operation K/P;
- Attempt 2 -> approval B -> same operation K/P;
- Attempt 3 -> approval C -> same operation K/P.

Concurrent replay pre-creates a distinct valid approval for every caller before releasing the concurrency barrier.

Payload conflict:

- Attempt 1 -> K + P1 + approval A;
- conflict attempt -> K + P2 + approval B;
- both approvals are valid for their own payload;
- any rejection after that is attributable to the idempotency payload comparison, not approval mismatch.

## Driver audit answers

| Question | Stage 6 evidence |
|---|---|
| Does intended attempt reach ToolGateway? | Driver invokes the real `ToolGatewayService`. |
| Authorization valid? | Real gateway trusted authorization path remains active; no bypass added. |
| Schema valid? | Real `ToolInputSchemaValidator` remains active. |
| Business scope valid? | Real scope validators remain active. |
| Approval valid? | Fresh approval per attempt through real create/approve lifecycle. |
| Write executor reached? | Per-attempt attribution classifier records boundary reachability. |
| WriteOperationService reached? | Required for attribution eligibility. |
| External transport executed? | Counted independently by `RecordingRefundExternalSystem`. |
| Early block cause? | Classified as authorization/schema/scope/approval/unknown pre-idempotency block. |

## Evidence level in this environment

- Static audit: PASS.
- Isolated Java compilation of modified driver/evaluator dependency set: PASS.
- Pure attribution classifier/eligibility harness: PASS.
- Spring/JUnit driver contract tests: NOT RUN (Maven/Maven Wrapper unavailable).
- Held-out Idempotency benchmark: NOT RUN.
