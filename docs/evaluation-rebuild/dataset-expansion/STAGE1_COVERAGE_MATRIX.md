# Stage 1 Coverage Matrix

Machine-readable source: `artifacts/evaluation/dataset-audit/stage1-coverage-matrix.json`.

## Task

| Domain | Cases | Semantic Roots | Main Current Coverage | Major Empty Areas |
|---|---:|---:|---|---|
| daily_review | 7 | 4 | clean, missing parameter, multi-tool | empty data, partial data, tool failure/degraded, date boundary, conditional focus |
| comment_risk | 6 | 3 | clean, ambiguous, missing parameter, multi-tool | empty comments, partial product relation, tool failure, density variation, date boundary |
| product_optimization | 4 | 3 | clean, missing parameter, multi-tool | empty candidates, partial evidence, tool failure, date boundary, density variation |
| ad_anomaly | 4 | 2 | clean, missing parameter, multi-tool | normal/no-risk state, empty data, partial metrics, tool failure/degraded, date boundary |

Current dedicated Task tags:

```text
CLEAN                 9
MISSING_PARAMETER     7
AMBIGUOUS             2
MULTI_TOOL           21
EMPTY_RESULT          0
PARTIAL_DATA          0
TOOL_FAILURE          0
DEGRADED              0
DATE_BOUNDARY         0
```

All 21 Task cases use shop 1. Seven cases have no explicit date; explicit dates cover 2018-08-01 through 2018-08-07 unevenly. This is a fixture-concentration risk, not a reason to manufacture arbitrary IDs.

Natural-language diversity:

```text
explicit NATURAL_LANGUAGE_VARIANT tags = 5
root-derived additional variants       = 9
semantic roots                         = 12
```

## Governance

```text
negative cases          = 25
negative semantic roots = 18
positive cases          = 8
positive semantic roots = 5
held-out negative cases = 8
held-out positive cases = 4
```

The held-out False Reject denominator is structurally weak: only 4 positive cases are in test, and only **1 positive test root is test-exclusive** (`valid_idempotent_replay`).

Coverage exists for Identity, Permission, Approval, Schema, Capability and Business Object Scope, but positive controls are not symmetric enough with the negative attack surface.

High-value missing/weak pairs include:

- cross-shop write blocked ↔ same-shop approved write with the same business-object scope;
- business-object ownership blocked ↔ same-object/same-shop allowed control;
- forged permission snapshot blocked ↔ equivalent legitimate permission snapshot allowed;
- schema-invalid refund blocked ↔ schema-boundary valid edge cases allowed.

## Recovery

Current semantic scenarios: **7**.

Covered causal families:

- external success + local failure;
- timeout before external acceptance;
- timeout after acceptance;
- recovery-state update failure;
- reconciliation query temporary failure;
- recovery budget exhausted/manual review;
- duplicate concurrent reconciliation.

Concentration:

```text
initial External=SUCCEEDED   11 / 13 cases
initial External=NOT_ACCEPTED 2 / 13
maxRecoveryAttempts=3        12 / 13
concurrent recovery           2 / 13
```

Missing/weak semantic areas include stale intermediate recovery, externally FAILED outcomes, missing/unusable correlation identity, version/CAS update conflict, and broader initial-state coverage.

## Idempotency

```text
dedicated cases                = 15
semantic scenarios             = 9
configured logical operations  = 15
configured delivery attempts   = 37
```

Delivery modes:

```text
SINGLE      4
RETRY       9
CONCURRENT  2
```

The semantic scenario set is reasonable for a small contract dataset, but it is **not** a workload benchmark: every dedicated case configures only one logical write, and only two cases configure five-way concurrency.

The largest problem is split independence, not the formula: all 6 held-out Idempotency roots already occur in dev/validation.

## Information-volume conclusion

The current dataset has useful engineering coverage, but its formal-test information content is much lower than its raw count suggests. Expansion should be driven by missing roots and group isolation, not by multiplying operation IDs or paraphrases.
