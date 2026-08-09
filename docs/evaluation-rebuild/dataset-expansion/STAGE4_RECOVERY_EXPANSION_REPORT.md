# Stage 4 Recovery Expansion Report

## Before / After

| Metric | Stage 1 / Stage 3 baseline | Stage 4 candidate |
|---|---:|---:|
| Dedicated cases | 13 | **21** |
| Semantic / causal roots | 7 | **15** |
| Historical held-out cases | 7 | — |
| Candidate held-out cases | — | **6** |
| True test-exclusive roots | 1 | **6** |
| Cross-split leaked roots | 6 | **0** |
| External SUCCEEDED roots | 6 | **11** |
| External NOT_ACCEPTED roots | 1 | **4** |
| Initial EXECUTING roots | 3 | **5** |
| Initial EXTERNAL_UNKNOWN roots | 4 | **10** |
| Manual-review roots | 1 | **3** |
| Concurrency roots | 1 | **3** |

Case/root ratio changed from `13 / 7 = 1.86` to `21 / 15 = 1.40`, so expansion increased independent causal information faster than raw cases.

## New causal coverage

The eight accepted roots add:

- temporary query failure followed by definitive `NOT_ACCEPTED`;
- temporary query failure from an `EXTERNAL_UNKNOWN` response-loss checkpoint;
- `LAST_ALLOWED_SUCCESS` budget boundary;
- budget exhaustion with external `NOT_ACCEPTED`;
- budget exhaustion from `EXTERNAL_UNKNOWN` while true external state is `SUCCEEDED`;
- recovery state-update failure starting from `EXECUTING`;
- concurrent recovery from `EXECUTING`;
- concurrent recovery to `FAILED` when external reality is `NOT_ACCEPTED`.

No new root is based solely on seed, request ID, refund amount, retry count, or worker count.

## Manual review semantics

Three roots expect manual review. They are not reclassified as automatic convergence.

When true external reality is `SUCCEEDED` or `NOT_ACCEPTED` but local recovery cannot observe it before budget exhaustion, local `NEEDS_MANUAL_ACTION` is terminal but `expectedConvergence=false`.

## External truth gap

Stage 4 still has no accepted `FAILED` or final `UNKNOWN` external-reality root because the current independent test external system cannot establish those causal sequences without new fixture behavior. This is explicitly retained as a gap.

## Gold consistency

Seven old Phase 6 recovery cases had stale `businessTarget` constraints after their `orderId` was migrated to the real seed order. Stage 4 corrected those fields and therefore increments the Recovery candidate Gold version.
