# Stage 2 Task Expansion Report

## Before vs after

| Metric | Stage 1 | Stage 2 Candidate |
|---|---:|---:|
| Dedicated TASK cases | 21 | **93** |
| TASK semantic roots | 12 | **52** |
| Held-out TASK cases | 6 | **43** |
| Held-out semantic roots | 6 | **23** |
| Truly test-exclusive roots | 3 | **23** |
| Cross-split TASK root leaks | 3 | **0** |

Stage 2 added **72 cases representing 40 new semantic roots**, not 72 independent roots.

## Split

| Split | Cases | Semantic Roots |
|---|---:|---:|
| dev | 27 | 15 |
| validation | 23 | 14 |
| test | 43 | 23 |

All test roots are root-level exclusive after the split repair.

## Business coverage

| Scenario | Cases | Roots | Test Roots |
|---|---:|---:|---:|
| daily_review | 25 | 13 | 6 |
| comment_risk | 24 | 12 | 6 |
| product_optimization | 24 | 14 | 6 |
| ad_anomaly | 20 | 13 | 5 |

## Failure/business-state coverage

| Tag | Cases | Roots |
|---|---:|---:|
| CLEAN | 13 | 11 |
| AMBIGUOUS | 2 | 1 |
| MISSING_PARAMETER | 7 | 4 |
| EMPTY_RESULT | 18 | **10** |
| PARTIAL_DATA | 12 | **6** |
| TOOL_FAILURE | 0 | 0 |
| DEGRADED | 0 | 0 |
| DATE_BOUNDARY | 8 | **4** |
| MULTI_TOOL | 93 | 52 |
| LOW_DATA_DENSITY | 27 | 15 |
| HIGH_DATA_DENSITY | 25 | 14 |

The zero TOOL_FAILURE / DEGRADED count is intentional: candidates requiring degraded-success semantics were rejected because the current evaluator cannot distinguish them reliably without evaluator changes.

## Near-duplicate review

The deterministic detector produced 213 lexical/structural candidates:

- reviewed: 213
- SAME_ROOT: 43
- KEEP_DISTINCT: 170
- unresolved high-risk: **0**
- cross-split lexical candidates: 119

Candidate count is not forced to zero. Cross-split pairs are retained only when the review can explain the different semantic root using fixture facts/business state rather than wording alone.
