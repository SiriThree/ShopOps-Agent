# Phase 3 Results

## Formal result status

```text
Phase 1/2 Spring Runtime Gate      NOT PASSED
Phase 3 Spring/JUnit Integration  NOT RUN
JDBC/MySQL Idempotency            NOT RUNTIME VERIFIED
RabbitMQ Redelivery               NOT RUN / refund write path not directly queued
Formal Duplicate Side Effects     NOT AVAILABLE
```

No formal duplicate count is reported as `0`.

## Actually executed checks

### Repository/resource validation

```text
benchmark JSON cases validated     42
unique caseId                      42
schema errors                       0
Phase 3 idempotency cases          14
configured post-approval deliveries 35 (dataset design only; not executed as formal run)
phase8 static validation           21 PASS / 0 FAIL
Phase 3 pure contract javac        PASS / 15 classes
```

### Pure external-ground-truth harness

Executed with Java 21 against `RecordingRefundExternalSystem` + production metric formula:

```text
PHASE3_PURE_EXTERNAL_LEDGER_HARNESS PASS
```

It proved only the external test-system semantics: non-idempotent mode records two accepted calls as two effects; idempotent comparison records two attempts as one effect; timeout-after-success records one effect while returning UNKNOWN. This is not a ShopOps end-to-end result.

### Production write/executor deterministic harness

A javac harness compiled and executed the actual production:

- `WriteOperationService` memory path;
- `HighRiskRefundExecuteExecutor`;
- `RefundExternalClient`;
- `RefundExternalTransport` boundary;
- independent non-idempotent recording external system.

Spring/DB infrastructure types were temporary compile stubs only; business/write classes above were the repository production sources. Results:

```text
CONCURRENT_RETRY
  deliveries=5
  externalAttempts=1
  effectiveEffects=1
  success=1
  inProgress=4
  PASS (PURE / production write-executor level)

SEQUENTIAL_RETRY
  deliveries=2
  externalAttempts=1
  effectiveEffects=1
  PASS

TIMEOUT_AFTER_SUCCESS
  deliveries=2
  externalAttempts=1
  effectiveEffects=1
  PASS

PAYLOAD_CONFLICT
  deliveries=2
  externalAttempts=1
  effectiveEffects=1
  rejectedConflicts=1
  PASS

EXTERNAL_SUCCESS_LOCAL_FAILURE
  deliveries=2
  externalAttempts=1
  effectiveEffects=1
  retryBlocked=1
  PASS
```

These are valuable implementation checks, but **not formal Tool-Gateway/Spring/JDBC benchmark numbers**.

### Idempotency evaluator harness

The actual Phase 3 `SideEffectIdempotencyEvaluator` was compiled with its repository contracts/domain DTOs and executed against one approved, correctly targeted external effect:

```text
PHASE3_IDEMPOTENCY_EVALUATOR_HARNESS PASS
```

This validates evaluator semantics only, not the Spring runtime.

### Production instrumentation compilation

The modified production write/fault instrumentation was compiled with Java 21 plus minimal temporary framework stubs:

```text
PHASE3_PRODUCTION_INSTRUMENTATION_JAVAC PASS
compiled classes = 32
```

This validates source compatibility at the Java language/type boundary represented by the stubs, not a Maven build.

## Raw formal counts

Because the formal runtime gate has not executed:

```text
Logical Write Requests        NOT AVAILABLE
Delivery Attempts             NOT AVAILABLE
Execution Attempts            NOT AVAILABLE
Tool Attempts                 NOT AVAILABLE
External Attempts             NOT AVAILABLE
Expected Effective Effects    NOT AVAILABLE
Actual Effective Effects      NOT AVAILABLE
Duplicate Side Effects        NOT AVAILABLE
Missing Side Effects          NOT AVAILABLE
```

The PURE harness counts above are intentionally kept separate.

## Maven/runtime attempt

The repository still has no Maven Wrapper. `mvn`, Docker, and PowerShell are not present in the environment. An `apt-get update && apt-get install maven` attempt timed out, so JUnit/Spring execution could not be started. Tests that require those runtimes remain `NOT RUN`.
