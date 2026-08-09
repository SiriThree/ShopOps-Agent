# Stage 2 Task Split Repair

## Scope

Stage 2 modifies only TASK benchmark resources and benchmark-only metadata/test infrastructure. It does not change production runtime or task outcome evaluators.

## Stage 1 leaked roots

Stage 1 identified three TASK semantic roots that crossed a development split and `test`, so their old test variants were not valid root-level held-out cases.

| Semantic Root | Old Test Case | Old Split | New Split | Reason |
|---|---|---|---|---|
| `task:daily_review:safe-default` | `test-task-daily-missing-date-001` | test | dev | Root already existed in dev; contaminated for held-out use. |
| `task:ad_anomaly:date:2018-08-07:2018-08-07` | `test-task-ad-risk-001` | test | dev | Root already existed in dev; contaminated for held-out use. |
| `task:comment_risk:safe-default` | `test-task-comment-ambiguous-001` | test | validation | Root already existed in validation; contaminated for held-out use. |

The reassigned cases keep their original `caseId` for lineage, lose `HELD_OUT`, and gain `CONTAMINATED_FOR_HELD_OUT` and `REASSIGNED_STAGE2` tags.

## Root split plan

`benchmark/v1/task/stage2/task-root-split-plan.json` assigns all 52 current TASK semantic roots to exactly one of `dev`, `validation`, or `test` before variants are interpreted as independent dataset members.

Current root counts:

- dev: 15 roots
- validation: 14 roots
- test: 23 roots

## Result

- Stage 1 leaked TASK roots: 3
- Reassigned contaminated roots: 3
- Remaining TASK cross-split semantic-root leakage: **0**
- Remaining TASK cross-split parent leakage: **0**
- Old held-out claim for the three moved cases: **invalidated**
- Current test roots: **23 / 23 test-exclusive**

No split assignment used Agent pass/fail behavior.
