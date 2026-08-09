# Stage 2 Handoff

## What changed

Stage 2 repaired TASK root-level split leakage and expanded only the TASK dataset. Production runtime and formal outcome evaluator semantics were not changed.

## Current TASK dataset

- 93 dedicated cases
- 52 semantic roots
- dev: 27 cases / 15 roots
- validation: 23 / 14
- test: 43 / 23
- test-exclusive roots: 23
- cross-split root leakage: 0
- cross-split parent leakage: 0

## Split repair

Three Stage 1 contaminated test cases were reassigned to dev/validation and are no longer held-out:

- `test-task-daily-missing-date-001` → dev
- `test-task-ad-risk-001` → dev
- `test-task-comment-ambiguous-001` → validation

## Expansion

- proposed roots: 50
- accepted: 40
- rejected: 10
- new cases: 72

Rejected roots expose two remaining dataset/evaluator boundaries:

1. TOOL_FAILURE / DEGRADED business outcomes are not reliably expressible by current evaluators.
2. wall-clock-relative date roots are not deterministic without a controlled Clock.

## Coverage gained

- EMPTY_RESULT: 10 roots
- PARTIAL_DATA: 6 roots
- DATE_BOUNDARY: 4 roots
- LOW_DATA_DENSITY: 15 roots
- HIGH_DATA_DENSITY: 14 roots

TOOL_FAILURE and DEGRADED remain at zero roots intentionally.

## Review truth

All 72 new cases are `MODEL_REVIEWED` and `humanReviewed=false`. There is no evidence-backed HUMAN_REVIEWED case in the repository. Historical 21 Task `humanReviewed=true` flags are retained but treated as uncertain provenance.

## Held-out discipline

New test roots have not been run against the Agent in Stage 2. Only schema, root isolation, Gold, fixture/static, duplicate, and candidate-manifest validation are permitted before a future formal freeze/run.

## Recommended next stage

Do not continue bulk Task expansion merely to raise the case count. The Task dataset now has 52 roots and 23 root-level held-out roots. Remaining TOOL_FAILURE/DEGRADED gaps are evaluator-contract gaps, not a lack-of-paraphrases problem.

The next dataset-engineering stage should move to **Governance**, especially root-level split repair and positive-control expansion, because Stage 1 showed the False Reject denominator and test-exclusive positive-control roots are much weaker than the negative attack coverage.
