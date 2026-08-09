# Stage 1 Leakage Audit

## Summary

| Check | Count | Status |
|---|---:|---|
| Exact runtime-payload duplicate pairs | 0 | PASS |
| Normalized runtime-payload duplicate pairs | 0 | PASS |
| Exact input + key-Gold signature pairs | 0 | PASS |
| Normalized input + key-Gold signature pairs | 0 | PASS |
| Cross-split same semantic root | 25 roots | **RISK** |
| Cross-split parent lineage | 0 | PASS |
| Near-duplicate cross split | 27 pairs | **RISK** |
| Production case-id references | 0 | PASS |
| Held-out input references in evaluator code | 0 | PASS |
| `expectedToolCodes` in dedicated Task Gold | 0 | PASS |

## Cross-split semantic-root leakage

| Semantic Root | Splits | Cases |
|---|---|---|
| `governance:approval_replay` | test, validation | test: `gov-test-approval-replay`<br>validation: `gov-val-approval-replay-control` |
| `governance:approval_target_mismatch` | test, validation | test: `gov-test-approval-target-mismatch`<br>validation: `gov-val-approval-target-mismatch-control` |
| `governance:cross_shop_refund` | dev, test | dev: `gov-dev-cross-shop-refund`<br>test: `gov-test-cross-shop-refund` |
| `governance:cross_tenant_refund` | dev, test | dev: `gov-dev-cross-tenant-refund`<br>test: `gov-test-cross-tenant-refund` |
| `governance:schema_wrong_type` | test, validation | test: `gov-test-schema-wrong-type`<br>validation: `gov-val-schema-wrong-type` |
| `governance:unknown_tool` | dev, test | dev: `gov-dev-unknown-tool`<br>test: `gov-test-unknown-tool` |
| `governance:valid_approved_refund` | test, validation | test: `gov-test-valid-approved-refund`<br>validation: `gov-val-valid-approved-refund-control` |
| `governance:valid_high_risk_preapproval` | test, validation | test: `gov-test-valid-preapproval`<br>validation: `gov-val-valid-preapproval` |
| `governance:valid_mcp_read` | test, validation | test: `gov-test-valid-mcp-read`<br>validation: `gov-val-valid-mcp-read-control` |
| `governance:viewer_refund_permission` | dev, test | dev: `gov-dev-viewer-refund`<br>test: `gov-test-viewer-refund` |
| `idempotency:baseline_single_delivery` | dev, test | dev: `idem-dev-baseline-001`<br>test: `idem-test-baseline-001` |
| `idempotency:concurrent_retry` | dev, test | dev: `idem-dev-concurrent-retry-001`<br>test: `idem-test-concurrent-retry-001` |
| `idempotency:external_success_local_failure` | test, validation | test: `idem-test-external-success-local-failure-001`<br>validation: `idem-val-external-success-local-failure-001` |
| `idempotency:same_key_different_payload` | dev, test | dev: `idem-dev-payload-conflict-001`<br>test: `idem-test-payload-conflict-001` |
| `idempotency:sequential_retry` | dev, test | dev: `idem-dev-sequential-retry-001`<br>test: `idem-test-sequential-retry-001` |
| `idempotency:timeout_after_external_success` | dev, test | dev: `idem-dev-timeout-after-success-001`<br>test: `idem-test-timeout-after-success-001` |
| `recovery:duplicate_reconciliation` | test, validation | test: `recovery-test-r8-duplicate-reconciliation`<br>validation: `recovery-val-r8-duplicate-reconciliation` |
| `recovery:external_success_local_failure` | dev, test | dev: `recovery-dev-r1-external-success-local-failure`<br>test: `recovery-test-r1-request-correlation` |
| `recovery:reconciliation_temporary_failure` | test, validation | test: `recovery-test-r5-query-temporary-failure`<br>validation: `recovery-val-r5-query-temporary-failure` |
| `recovery:recovery_budget_exhausted` | test, validation | test: `recovery-test-r6-budget-exhausted`<br>validation: `recovery-val-r6-budget-exhausted` |
| `recovery:timeout_after_external_acceptance` | dev, test | dev: `recovery-dev-r3-timeout-after-accept`<br>test: `recovery-test-r3-timeout-after-accept` |
| `recovery:timeout_before_external_acceptance` | dev, test | dev: `recovery-dev-r2-timeout-before-accept`<br>test: `recovery-test-r2-timeout-before-accept` |
| `task:ad_anomaly:date:2018-08-07:2018-08-07` | dev, test | dev: `dev-task-ad-risk-001`<br>dev: `dev-task-ad-risk-002`<br>test: `test-task-ad-risk-001` |
| `task:comment_risk:safe-default` | test, validation | test: `test-task-comment-ambiguous-001`<br>validation: `validation-task-comment-missing-date-001`<br>validation: `validation-task-comment-ambiguous-001` |
| `task:daily_review:safe-default` | dev, test | dev: `dev-task-daily-missing-date-001`<br>test: `test-task-daily-missing-date-001` |

Total: **25 dedicated semantic roots** cross dev/validation/test.

By benchmark:

```text
Task          3
Idempotency   6
Recovery      6
Governance   10
```

## Why caseId uniqueness was insufficient

The existing case IDs are unique, and exact runtime payload duplication is zero. Nevertheless, dev/validation/test repeatedly encode the same business objective, fault semantics or security boundary with new IDs.

The existing `semanticTaskId` field is also insufficient:

- one existing ID (`missing-approval`) incorrectly spans two distinct Governance roots;
- seven Stage 1 roots are fragmented across multiple semanticTaskId values;
- Task naming misses all three detected Task root leaks;
- Governance validation-control IDs hide several test/validation overlaps.

## Gold leakage

No current `shopops-admin/src/main/java` source contains a benchmark case ID.

No exact held-out Task input string was found in the Task evaluator source tree.

No dedicated Task Gold contains `expectedToolCodes`.

This is evidence against direct current-code leakage, but it does not prove historical legacy-Gold derivation. The 8 `LEGACY_MIGRATED` cases remain provenance-limited.

## Required migration behavior

Stage 1 intentionally does not move cases. The next dataset migration must group by `semanticRootId` and move an entire root family together. Split repair requires a dataset version increment and a new manifest; the current Phase 6 manifest should be treated as an **expansion baseline**, not as an independence-certified final manifest.
