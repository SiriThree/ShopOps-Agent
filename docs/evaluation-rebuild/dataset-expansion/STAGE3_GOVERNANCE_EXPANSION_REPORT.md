# Stage 3 Governance Expansion Report

## Before / After

| Measure | Stage 1 baseline | Stage 3 candidate |
|---|---:|---:|
| Dedicated cases | 33 | **56** |
| Semantic roots | 23 | **46** |
| Negative cases | 25 | **31** |
| Negative roots | 18 | **24** |
| Positive cases | 8 | **25** |
| Positive roots | 5 | **22** |
| Held-out cases | 12 | **18** |
| True test-exclusive roots | 2 | **18** |
| True test-exclusive negative roots | 1 | **6** |
| True test-exclusive positive roots | 1 | **12** |
| Cross-split root leakage | 10 | **0** |
| Cross-split parent leakage | 0 | **0** |

The gain is therefore not only +23 cases. It is +23 accepted roots, a root-level held-out set, and a materially larger positive denominator.

## Final split

| Split | Cases | Semantic roots |
|---|---:|---:|
| dev | 15 | 11 |
| validation | 23 | 17 |
| test | 18 | 18 |
| **Total** | **56** | **46** |

All 18 test roots are test-exclusive.

## Positive / Negative

- negative: **31 cases / 24 roots**
- positive: **25 cases / 22 roots**
- test negative roots: **6**
- test positive roots: **12**
- positive expected decisions: **14 ALLOWED**, **11 REQUIRES_APPROVAL**
- held-out positive expected decisions: **6 ALLOWED**, **6 REQUIRES_APPROVAL**

`REQUIRES_APPROVAL` is treated as correct preservation of a legitimate high-risk request, not a False Reject.

## Near-duplicate review

The deterministic detector produced **555 candidates**. All 555 were reviewed through the Stage 3 metadata review contract:

- `KEEP_DISTINCT`: 512
- `KEEP_DISTINCT_PAIRED_CONTROL`: 33
- `SAME_ROOT`: 10
- unresolved high-risk: **0**

No candidate was deleted only because its lexical similarity was high.

## Formal-result warning

Stage 3 is a dataset-construction stage. New held-out Governance roots were not executed. No Unauthorized Block Rate or False Reject Rate is reported here.
