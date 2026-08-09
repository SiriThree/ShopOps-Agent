# Stage 4 Recovery Review Report

## Root author / critic results

```text
Proposed   17
Accepted    8
Rejected    9
```

Rejection taxonomy:

```text
REJECTED_NO_FIXTURE                    2
REJECTED_FAULT_POINT_UNAVAILABLE       3
REJECTED_EXTERNAL_TRUTH_UNAVAILABLE    2
REJECTED_NOT_SEMANTICALLY_DISTINCT     2
REJECTED_UNSUPPORTED_RUNTIME           0
REJECTED_GOLD_AMBIGUOUS                0
REJECTED_EVALUATOR_UNOBSERVABLE        0
REJECTED_NEAR_DUPLICATE                0
```

The critic rejected root proposals that required new test-external truth semantics, impossible correlation state, unavailable crash checkpoints, or mere workload variations.

## Near-duplicate review

```text
Structured candidates       59
Reviewed                    59
SAME_ROOT                    6
KEEP_DISTINCT               53
Cross-split candidates      35
Unresolved high-risk         0
```

Recovery similarity uses causal structure (`faultType`, `faultPoint`, initial state, external state, terminal expectation, budget and concurrency), not natural-language similarity.

The six `SAME_ROOT` pairs are historical variants kept in the same repaired split.

## Review truth

All current Recovery cases are represented as `MODEL_REVIEWED` in Stage 4 metadata.

```text
Evidence-backed HUMAN_REVIEWED = 0
```

The 13 historical `humanReviewed=true` flags are retained but not interpreted as evidence-backed human review.

## Gold corrections

The seven `businessTarget` corrections are review-time dataset consistency fixes. No Agent execution was used to choose the replacement; each value is required to match the case's own `input.orderId`.
