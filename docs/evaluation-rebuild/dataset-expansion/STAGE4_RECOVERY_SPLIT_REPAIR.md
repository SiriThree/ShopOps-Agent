# Stage 4 Recovery Split Repair

## Scope

Stage 4 uses the Stage 3 repository as its only baseline and changes only the dedicated Recovery dataset plus dataset-audit/test metadata. Production recovery runtime and `StateConvergenceEvaluator` are unchanged.

## Stage 1 contamination baseline

```text
Dedicated Recovery cases      13
Causal roots                   7
Historical held-out cases      7
True test-exclusive roots      1
Cross-split leaked roots       6
```

The six contaminated roots were:

- `recovery:external_success_local_failure`
- `recovery:timeout_before_external_acceptance`
- `recovery:timeout_after_external_acceptance`
- `recovery:reconciliation_temporary_failure`
- `recovery:recovery_budget_exhausted`
- `recovery:duplicate_reconciliation`

Only `recovery:recovery_state_update_failure` was already test-exclusive.

## Repair rule

A historical test variant whose causal root already appeared in dev/validation cannot remain held out. The case is retained, moved to the development split owning the root, and marked:

```text
CONTAMINATED_FOR_HELD_OUT
REASSIGNED_STAGE4
```

No split assignment used current PASS/FAIL behavior.

## Reassignments

| Case | Root | Old | New |
|---|---|---|---|
| `recovery-test-r1-request-correlation` | external success + local failure | test | dev |
| `recovery-test-r2-timeout-before-accept` | timeout before acceptance | test | dev |
| `recovery-test-r3-timeout-after-accept` | timeout after acceptance | test | dev |
| `recovery-test-r5-query-temporary-failure` | temporary reconciliation query failure | test | validation |
| `recovery-test-r6-budget-exhausted` | recovery budget exhausted | test | validation |
| `recovery-test-r8-duplicate-reconciliation` | concurrent reconciliation | test | validation |

After repair and Stage 4 additions:

```text
Recovery cross-split root leakage = 0
Cross-split parent leakage        = 0
```

## Gold consistency corrections

Stage 4 also corrected seven pre-existing held-out cases whose `sideEffectExpectation.constraints.businessTarget` still referenced an old synthetic order while `input.orderId` had already been migrated to `SO202607180001` in Phase 6.

Corrected cases:

- `recovery-test-state-update-failure`
- `recovery-test-r1-request-correlation`
- `recovery-test-r2-timeout-before-accept`
- `recovery-test-r3-timeout-after-accept`
- `recovery-test-r5-query-temporary-failure`
- `recovery-test-r6-budget-exhausted`
- `recovery-test-r8-duplicate-reconciliation`

This is a dataset-contract consistency correction under the new Stage 4 candidate version. It was not derived from Agent output.
