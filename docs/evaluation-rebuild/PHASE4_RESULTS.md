# ShopOpsBench Recovery v1 — Phase 4 Results

## Result status

```text
Implementation status                IMPLEMENTED
PURE/MEMORY production recovery      EXECUTED
Spring/JUnit runtime                 NOT RUN
JDBC/MySQL recovery                  NOT RUNTIME VERIFIED
Formal State Convergence Rate        NOT AVAILABLE
```

A formal rate is intentionally withheld because the current environment cannot run Maven/Spring/JDBC. PURE/MEMORY evidence is reported separately below.

## Dataset

Phase 4 dedicated recovery dataset:

```text
dev          3
validation   3
test         2
----------------
total        8 semantic recovery cases
```

The whole versioned benchmark resource tree contains 50 cases with 50 unique case IDs and zero schema errors. There is also one older Phase 0 contract-level `RECOVERY` example outside the Phase 4 dedicated recovery split, so repository-wide `benchmarkType=RECOVERY` count is 9.

The Phase 4 held-out `test` split was not used to manufacture development results.

## Actually executed PURE/MEMORY recovery scenarios

Execution level:

```text
Production write executor + WriteOperationService + RefundExternalClient
+ WriteOperationReconciliationService
Persistence: MEMORY
External system: IN_PROCESS_TEST_DOUBLE / NON_IDEMPOTENT_EXTERNAL
```

Actual observations:

| Scenario | Terminal? | External/local correct? | Recovery attempts | Manual review | Effective effects | Duplicate effects |
|---|---:|---:|---:|---:|---:|---:|
| R1 external success + local failure | yes | yes | 1 | 0 | 1 | 0 |
| R2 timeout before acceptance | yes | yes | 1 | 0 | 0 | 0 |
| R3 timeout after acceptance | yes | yes | 1 | 0 | 1 | 0 |
| R5 temporary reconciliation-query failure | yes | yes | 2 | 0 | 1 | 0 |
| R6 recovery budget exhausted | yes | **no — safe manual terminal** | 3 | 1 | 1 | 0 |
| R8 duplicate concurrent reconciliation | yes | yes | 2 | 0 | 1 | 0 |

Raw PURE/MEMORY diagnostic counts:

```text
Fault scenarios actually executed      6
Terminal reached                       6
External/local state correct           5
Converged observations                 5
Permanent stuck                        0
Incorrect terminal state               0
Manual review                           1
Recovery attempts                      10
Effective external side effects         5
Duplicate external side effects         0
```

These are execution-level diagnostics, **not the formal ShopOpsBench State Convergence Rate**. R6 intentionally demonstrates bounded failure handling: external truth is `SUCCEEDED`, but repeated external-status-query failures exhaust the automatic recovery budget and produce `NEEDS_MANUAL_ACTION`. It is terminal and safe, but it is not automatic external/local convergence.

## R1 recovery-gap verification

Before recovery:

```text
Local state       EXECUTING
externalReference null
External reality  SUCCEEDED
External effects  1
```

After one production reconciliation attempt:

```text
Local state       SUCCEEDED
Recovery attempts 1
External effects  1
Duplicate effects 0
```

The recovery path found the external operation through the durable pre-call `operationRequestId`; it did not submit a second refund.

## Concurrency regression found during Phase 4

A two-worker recovery run initially exposed:

```text
worker A: recovery → SUCCEEDED
worker B: stale snapshot / exception path → attempts SUCCEEDED → NEEDS_RECONCILIATION
```

This correctly failed with an illegal transition. Production was fixed to re-read the latest operation before recovery/unresolved transitions and to keep terminal states immutable. Re-running the same two-worker scenario produced:

```text
Final local state       SUCCEEDED
Recovery attempts       2
Effective effects       1
Duplicate effects       0
```

## Formal metrics

Because the Formal Gate is not met in this environment:

```text
Fault cases                   NOT AVAILABLE (formal)
Terminal Convergence Rate     NOT AVAILABLE
State Correctness Rate        NOT AVAILABLE
Permanent Stuck Rate          NOT AVAILABLE
Automatic Recovery Rate       NOT AVAILABLE
Manual Review Rate            NOT AVAILABLE
```

No `0%`, `100%`, or synthetic formal percentage is reported.

## Validation actually executed

```text
python3 scripts/phase8-static-validate.py
TOTAL=21 PASS=21 FAIL=0

Benchmark JSON/schema validation
50 cases
50 unique caseId
0 schema errors

Java 21 production recovery harness
R1 PASS
R2 PASS
R3 PASS
R5 PASS
R6 PASS (expected manual terminal)
R8 PASS
36 compiled classes
```

Spring/JUnit, MySQL/JDBC, RabbitMQ, and external commercial API tests remain `NOT RUN` / unavailable as documented in the handoff.

## Phase 3 idempotency regression after recovery changes

The same current production write code was re-executed with a deterministic Java harness after the Phase 4 recovery changes:

```text
SEQUENTIAL_RETRY                       PASS   effective effects=1
CONCURRENT_RETRY                       PASS   effective effects=1, external attempts=1
PAYLOAD_CONFLICT                       PASS   IDEMPOTENCY_PAYLOAD_MISMATCH, effects=1
TIMEOUT_AFTER_SUCCESS retry block      PASS   RECOVERY_REQUIRED, effects=1
EXTERNAL_SUCCESS_LOCAL_FAILURE replay  PASS   OPERATION_IN_PROGRESS, effects=1
```

This is PURE/MEMORY regression evidence. It shows the recovery fix did not solve convergence by permitting a second external refund.
