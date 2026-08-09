# Stage 1 Near-Duplicate Report

## Summary

```text
Exact runtime-payload pairs        = 0
Normalized runtime-payload pairs   = 0
Exact input+key-Gold pairs          = 0
Normalized input+key-Gold pairs     = 0
Near-duplicate candidates          = 33
Near-duplicate cross-split pairs   = 27
```

The candidate detector never deletes cases. Same-root lineage is treated as a review candidate even when lexical similarity is low, which is necessary for Chinese/English paraphrases and structured fault/security cases.

| Case A | Case B | Split | Lexical Similarity | Semantic Root | Decision |
|---|---|---|---:|---|---|
| `gov-dev-cross-shop-refund` | `gov-test-cross-shop-refund` | dev→test | 0.000 | `governance:cross_shop_refund ` | SAME_ROOT |
| `gov-dev-cross-tenant-refund` | `gov-test-cross-tenant-refund` | dev→test | 0.000 | `governance:cross_tenant_refund ` | SAME_ROOT |
| `gov-dev-unknown-tool` | `gov-test-unknown-tool` | dev→test | 0.000 | `governance:unknown_tool ` | SAME_ROOT |
| `gov-dev-viewer-refund` | `gov-test-viewer-refund` | dev→test | 0.000 | `governance:viewer_refund_permission ` | SAME_ROOT |
| `gov-test-approval-replay` | `gov-val-approval-replay-control` | test→validation | 0.000 | `governance:approval_replay ` | SAME_ROOT |
| `gov-test-approval-target-mismatch` | `gov-val-approval-target-mismatch-control` | test→validation | 0.000 | `governance:approval_target_mismatch ` | SAME_ROOT |
| `gov-test-schema-wrong-type` | `gov-val-schema-wrong-type` | test→validation | 0.000 | `governance:schema_wrong_type ` | SAME_ROOT |
| `gov-test-valid-approved-refund` | `gov-val-valid-approved-refund-control` | test→validation | 0.000 | `governance:valid_approved_refund ` | SAME_ROOT |
| `gov-test-valid-mcp-read` | `gov-val-valid-mcp-read-control` | test→validation | 0.000 | `governance:valid_mcp_read ` | SAME_ROOT |
| `gov-test-valid-preapproval` | `gov-val-valid-preapproval` | test→validation | 0.000 | `governance:valid_high_risk_preapproval ` | SAME_ROOT |
| `idem-dev-baseline-001` | `idem-test-baseline-001` | dev→test | 0.000 | `idempotency:baseline_single_delivery ` | SAME_ROOT |
| `idem-dev-concurrent-retry-001` | `idem-test-concurrent-retry-001` | dev→test | 0.000 | `idempotency:concurrent_retry ` | SAME_ROOT |
| `idem-dev-payload-conflict-001` | `idem-test-payload-conflict-001` | dev→test | 0.000 | `idempotency:same_key_different_payload ` | SAME_ROOT |
| `idem-dev-sequential-retry-001` | `idem-test-sequential-retry-001` | dev→test | 0.000 | `idempotency:sequential_retry ` | SAME_ROOT |
| `idem-dev-timeout-after-success-001` | `idem-test-timeout-after-success-001` | dev→test | 0.000 | `idempotency:timeout_after_external_success ` | SAME_ROOT |
| `idem-test-external-success-local-failure-001` | `idem-val-external-success-local-failure-001` | test→validation | 0.000 | `idempotency:external_success_local_failure ` | SAME_ROOT |
| `recovery-dev-r1-external-success-local-failure` | `recovery-test-r1-request-correlation` | dev→test | 0.000 | `recovery:external_success_local_failure ` | SAME_ROOT |
| `recovery-dev-r2-timeout-before-accept` | `recovery-test-r2-timeout-before-accept` | dev→test | 0.000 | `recovery:timeout_before_external_acceptance ` | SAME_ROOT |
| `recovery-dev-r3-timeout-after-accept` | `recovery-test-r3-timeout-after-accept` | dev→test | 0.000 | `recovery:timeout_after_external_acceptance ` | SAME_ROOT |
| `recovery-test-r5-query-temporary-failure` | `recovery-val-r5-query-temporary-failure` | test→validation | 0.000 | `recovery:reconciliation_temporary_failure ` | SAME_ROOT |
| `recovery-test-r6-budget-exhausted` | `recovery-val-r6-budget-exhausted` | test→validation | 0.000 | `recovery:recovery_budget_exhausted ` | SAME_ROOT |
| `recovery-test-r8-duplicate-reconciliation` | `recovery-val-r8-duplicate-reconciliation` | test→validation | 0.000 | `recovery:duplicate_reconciliation ` | SAME_ROOT |
| `dev-task-ad-risk-001` | `test-task-ad-risk-001` | dev→test | 0.062 | `task:ad_anomaly:date:2018-08-07:2018-08-07 ` | SAME_ROOT |
| `dev-task-ad-risk-002` | `test-task-ad-risk-001` | dev→test | 0.129 | `task:ad_anomaly:date:2018-08-07:2018-08-07 ` | SAME_ROOT |
| `dev-task-daily-missing-date-001` | `test-task-daily-missing-date-001` | dev→test | 0.000 | `task:daily_review:safe-default ` | SAME_ROOT |
| `test-task-comment-ambiguous-001` | `validation-task-comment-ambiguous-001` | test→validation | 0.156 | `task:comment_risk:safe-default ` | SAME_ROOT |
| `test-task-comment-ambiguous-001` | `validation-task-comment-missing-date-001` | test→validation | 0.000 | `task:comment_risk:safe-default ` | SAME_ROOT |
| `dev-task-ad-risk-001` | `dev-task-ad-risk-002` | dev→dev | 0.059 | `task:ad_anomaly:date:2018-08-07:2018-08-07 ` | SAME_ROOT |
| `dev-task-comment-risk-001` | `dev-task-comment-risk-002` | dev→dev | 0.062 | `task:comment_risk:date:2018-08-02:2018-08-02 ` | SAME_ROOT |
| `dev-task-daily-review-001` | `dev-task-daily-review-002` | dev→dev | 0.082 | `task:daily_review:date:2018-08-01:2018-08-01 ` | SAME_ROOT |
| `dev-task-product-opt-001` | `dev-task-product-opt-002` | dev→dev | 0.055 | `task:product_optimization:date:2018-08-03:2018-08-03 ` | SAME_ROOT |
| `validation-task-comment-missing-date-001` | `validation-task-comment-ambiguous-001` | validation→validation | 0.000 | `task:comment_risk:safe-default ` | SAME_ROOT |
| `validation-task-daily-review-001` | `validation-task-daily-variant-001` | validation→validation | 0.057 | `task:daily_review:date:2018-08-04:2018-08-04 ` | SAME_ROOT |

## Interpretation

Most candidates are not byte-for-byte duplicates. They are cases that reuse the same underlying semantic root, often with different IDs, wording, seeds or control labels.

`SAME_ROOT` means the pair may remain as multiple variants **only if all variants stay in one split**. Stage 1 does not delete or move them.

Machine-readable full list: `artifacts/evaluation/dataset-audit/stage1-near-duplicate-candidates.json`.
