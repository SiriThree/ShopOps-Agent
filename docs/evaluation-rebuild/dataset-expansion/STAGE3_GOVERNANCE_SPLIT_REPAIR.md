# Stage 3 Governance Split Repair

## Scope

Stage 3 repairs Governance semantic-root grouping only. Production governance code, evaluators, Task, Idempotency, Recovery resources, and the Phase 6 frozen manifest are not modified.

## Baseline

Stage 1 found 10 Governance semantic roots crossing dev/validation/test. A root seen in dev or validation is treated as contaminated for held-out use and cannot remain test-exclusive evidence.

## Reassignment

| Semantic root | Historical placement | Stage 3 split | Reason |
|---|---|---|---|
| `governance:approval_replay` | validation + test | validation | already development-visible |
| `governance:approval_target_mismatch` | validation + test | validation | already development-visible |
| `governance:cross_shop_refund` | dev + test | dev | already development-visible |
| `governance:cross_tenant_refund` | dev + test | dev | already development-visible |
| `governance:schema_wrong_type` | validation + test | validation | already development-visible |
| `governance:unknown_tool` | dev + test | dev | already development-visible |
| `governance:valid_approved_refund` | validation + test | validation | positive root already development-visible |
| `governance:valid_high_risk_preapproval` | validation + test | validation | positive root already development-visible |
| `governance:valid_mcp_read` | validation + test | validation | positive root already development-visible |
| `governance:viewer_refund_permission` | dev + test | dev | already development-visible |

Moved historical cases are retained and tagged `CONTAMINATED_FOR_HELD_OUT` and `REASSIGNED_STAGE3`; `HELD_OUT` is removed. No case is deleted because it can still serve development/regression coverage.

## Result

- old leaked roots: **10**
- reassigned contaminated roots: **10**
- remaining Governance cross-split semantic-root leakage: **0**
- cross-split parent leakage: **0**
- Stage 3 test roots: **18**
- true test-exclusive roots: **18**

Split assignment was made from root identity, historical contamination, and coverage stratification only. No runtime PASS/FAIL result was used.
