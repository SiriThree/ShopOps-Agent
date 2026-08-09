# Stage 3 Handoff

## What Stage 3 changed

Governance data moved from 33 cases / 23 roots to a 56-case / 46-root candidate. Ten historical cross-split roots were repaired. Positive roots grew from 5 to 22 and true test-exclusive positive roots from 1 to 12. Production and evaluator code were not changed.

## Final candidate facts

- Governance cases: 56
- roots: 46
- negative: 31 cases / 24 roots
- positive: 25 cases / 22 roots
- test: 18 cases / 18 roots
- test-exclusive negative roots: 6
- test-exclusive positive roots: 12
- cross-split semantic-root leakage: 0
- cross-split parent leakage: 0
- pair rows: 19
- near-duplicate candidates: 555 / reviewed 555 / unresolved 0
- new Stage 3 cases: 23, all `MODEL_REVIEWED`, `humanReviewed=false`
- evidence-backed human-reviewed cases: 0

## Important limitations

1. New held-out roots were not executed.
2. False Reject Rate and Unauthorized Block Rate remain unavailable until Formal runtime execution.
3. Capability/unknown-tool has no test-exclusive negative root; a duplicate unknown alias was rejected and no deterministic disabled-tool fixture exists.
4. `product.update_title` approved-write and Feishu-sync positives were rejected because independent external side-effect truth is unavailable.
5. MCP read remains represented but not as a new test-exclusive positive root.
6. Authorization fixture metadata does not replace future JDBC authorization verification.

## Recommended next stage

Move to **Recovery Dataset Group Split Repair and causal-root expansion**, not another broad Governance wave. Governance now has 22 positive roots and 12 true test-exclusive positive roots, which materially improves the future False Reject denominator. Remaining Governance gaps are targeted rather than broad.

Recovery remains the more urgent dataset-independence problem from Stage 1: most held-out recovery roots were previously exposed in dev/validation and causal diversity is still limited. Stage 4 should first repair Recovery group split, then add new causal paths rather than new seeds.
