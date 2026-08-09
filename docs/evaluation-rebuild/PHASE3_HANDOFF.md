# Phase 3 Handoff

## State of implementation

Phase 3 has implemented the Side-Effect Idempotency contract, external ground truth, refund flagship driver, deterministic fault SPI, evaluator, raw-count aggregation, dataset, unified-runner routing, reporting, and regression/integration test code.

It has **not** passed the Maven/Spring/JDBC formal runtime gate in this execution environment. Therefore `Duplicate Side Effects` remains `NOT AVAILABLE` as a formal ShopOps benchmark result.

## Real flagship entry

```text
Execution Level = TOOL_GATEWAY
ToolGatewayService.invoke
  -> DefaultToolGatewayService
  -> ApprovalRequestService
  -> HighRiskRefundExecuteExecutor
  -> WriteOperationService
  -> RefundExternalClient
  -> RefundExternalTransport
```

The current natural-language Agent does not route to this write tool, so do not call this an Agent write benchmark.

## External truth

`RecordingRefundExternalSystem` is the Phase 3 independent ground truth. Formal ShopOps-self-idempotency cases use `NON_IDEMPOTENT_EXTERNAL`. It never reads local `WriteOperation` state.

## Production fixes discovered by benchmark work

1. **Memory first-write concurrency race** — fixed by atomic `ConcurrentHashMap.compute` and detached snapshots so exactly one caller can receive `freshExecution=true`.
2. **Semantic payload hash included approval execution metadata** — top-level `approvalId` is now excluded, so replay through another approval record does not create a false payload conflict; changed business payload is still rejected.
3. **External system was not independently observable** — extracted `RefundExternalTransport` beneath existing `RefundExternalClient`; normal simulation behavior remains via `SimulatedRefundExternalTransport`.
4. **Reliability fault boundaries were not reusable/observable** — added a no-op production `ReliabilityFaultController` SPI and explicit fault points around external call, external-success/local-confirm gap, post-local-confirm response loss, outbox mark, and reconciliation query.

No case-id production branches were added.

## Database guarantee

`V20__phase2_write_reliability.sql` already provides `UNIQUE KEY uk_write_operation_idempotency(idempotency_key)`. JDBC `prepare` also catches duplicate key and rereads/verifies input. Optimistic state transitions use expected status + version. `JdbcRefundIdempotencyIntegrationTest` is added but **NOT RUN** because Maven/MySQL are unavailable.

## Queue guarantee / limitation

RabbitMQ is used by Agent task dispatch and Outbox publication, but the flagship refund write itself is not consumed through a dedicated Rabbit refund-write consumer. Therefore Phase 3 does not claim real refund `ACK_FAILURE` or `MESSAGE_REDELIVERY`; repeated delivery cases are labeled deterministic simulated delivery at Tool Gateway. `BEFORE_OUTBOX_MARK_PUBLISHED` instrumentation exists for future outbox-specific recovery/idempotency work.

## Current dataset

- 14 idempotency cases total.
- dev 5 / validation 4 / held-out test 5.
- flagship operation: refund only.
- includes baseline, sequential retry, concurrent retry, payload conflict, timeout-before/after acceptance, external success + local failure, post-confirm response loss, and external-idempotent comparison.

## Tests to run when Maven becomes available

```bash
mvn -pl shopops-admin -am -Dtest='SideEffectLedgerTest,SideEffectIdempotencyEvaluatorTest,LogicalOperationIdentityTest,IdempotencyPayloadConflictTest,FaultInjectionContractTest,IdempotencyCaseValidationTest,IdempotencyReportAggregationTest,WriteOperationServiceMemoryModeTest' -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl shopops-admin -am -Dtest='SingleDeliveryWriteIntegrationTest,SequentialRetryIdempotencyIntegrationTest,ConcurrentRetryIdempotencyIntegrationTest,ExternalSuccessLocalFailureIntegrationTest,AfterLocalConfirmResponseLossIntegrationTest,SameKeyDifferentPayloadIntegrationTest,TimeoutAfterSuccessIdempotencyIntegrationTest,Phase3IdempotencyBenchmarkIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false test

# Requires the project's MySQL integration database:
mvn -pl shopops-admin -am -Dshopops.jdbc.it=true -Dtest=JdbcRefundIdempotencyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Then rerun legacy/Agent/Tool Gateway/Approval/WriteOperation/Outbox/MCP regression suites.

## Phase 4 direct reuse

Phase 4 State Convergence / Recovery Benchmark should reuse:

- `ReliabilityFaultPoint` / `ReliabilityFaultController`;
- `RecordingRefundExternalSystem` and its external effect/reference state;
- `WriteOperation` evidence and state machine;
- `WriteOperationReconciliationService`;
- `ShopOpsBenchmarkRunner`;
- `EvaluationRecord` / `EvidenceRef`;
- Phase 3 fault cases, especially `EXTERNAL_SUCCESS_LOCAL_FAILURE` and timeout ambiguity.

The major recovery gap already visible in current production code is that reconciliation skips refund operations with no `externalReference`. A crash after external acceptance but before the reference is locally persisted can therefore leave `EXECUTING` with independent external reality already `SUCCEEDED`. Phase 4 should measure this as a convergence problem rather than hiding it in Phase 3.
