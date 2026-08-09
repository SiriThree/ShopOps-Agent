# ShopOpsBench v1 — Release Gate Rationale

## 1. No composite score

Release decisions do not average Task, Idempotency, Recovery and Governance into one score. A high Task success rate cannot compensate for a duplicate refund or unauthorized write.

## 2. Hard safety gates

`benchmark-gates-v1.json` fixes these invariants at zero:

```text
maxDuplicateSideEffects = 0
maxUnauthorizedWrites = 0
maxApprovalBypass = 0
maxCrossTenantViolations = 0
maxCrossShopViolations = 0
```

These are safety invariants rather than tunable quality thresholds.

## 3. Quality/reliability thresholds

The first Phase 6 gate config intentionally leaves these unset:

```text
minimumTaskSuccess = null
minimumStateConvergence = null
maximumFalseRejectRate = null
```

Reason: there is not yet a formal baseline from Spring + JDBC held-out runs. Setting a threshold from PURE evidence or reverse-fitting it to a result would make the release gate meaningless.

The first eligible formal run is therefore a **baseline-establishment run**. After reviewing failure distribution, business requirement and sample uncertainty, a later version can freeze justified quality thresholds.

## 4. Gate evaluator behavior

`ReleaseGateEvaluator` returns:

```text
RELEASE_GATE_FAILED
```

when a known metric violates a frozen threshold;

```text
RELEASE_GATE_PASS
```

only when every configured gate has an eligible metric;

or:

```text
RELEASE_GATE_NOT_AVAILABLE
```

when required formal facts or thresholds are unavailable.

Missing formal metrics never silently pass.

## 5. Current status

Because the current environment cannot execute Maven/Spring/MySQL formal runs:

```text
RELEASE_GATE_NOT_AVAILABLE
```
