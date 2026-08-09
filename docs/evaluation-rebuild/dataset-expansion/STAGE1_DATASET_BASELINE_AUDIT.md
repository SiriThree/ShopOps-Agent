# Stage 1 Dataset Baseline Audit

## Scope

This audit uses the current Phase 6 repository as the only source of truth. It does **not** add Benchmark cases, modify Gold, move cases between splits, execute held-out formal tests, or change production runtime.

Machine evidence: `artifacts/evaluation/dataset-audit/stage1-audit.json`.

## Current information volume

| Benchmark | Dedicated Cases | Semantic Roots | Held-Out Cases | Held-Out Roots | Test-Exclusive Roots | Cross-Split Leaked Roots |
|---|---:|---:|---:|---:|---:|---:|
| Task | 21 | 12 | 6 | 6 | 3 | 3 |
| Idempotency | 15 | 9 | 6 | 6 | 0 | 6 |
| Recovery | 13 | 7 | 7 | 7 | 1 | 6 |
| Governance | 33 | 23 | 12 | 12 | 2 | 10 |
| **Total** | **82** | **51** | **31** | **31** | **6** | **25** |

The repository also contains 7 non-dedicated resources: 3 Phase 1 smoke cases and 4 Phase 0 contract examples. Therefore:

```text
All versioned case objects = 89
Dedicated benchmark cases = 82
Dedicated semantic roots   = 51
```

The effective information volume is much closer to **51 semantic roots** than to 82 independent tasks/scenarios.

## Split baseline

```text
TASK         dev 9 / validation 6 / test 6
IDEMPOTENCY  dev 5 / validation 4 / test 6
RECOVERY     dev 3 / validation 3 / test 7
GOVERNANCE   dev 9 / validation 12 / test 12
```

## Held-out independence risk

The most important result is not the raw 31 held-out cases. Only **6 semantic roots are exclusive to test**:

```text
Task         3 / 6 test roots exclusive
Idempotency  0 / 6
Recovery     1 / 7
Governance   2 / 12
```

A formal score computed on the current split would therefore have a serious semantic leakage risk even though case IDs are unique.

## Existing `semanticTaskId` is not a sufficient split key

The old field is useful provenance, but it is not a reliable group-split contract.

Stage 1 found:

- one false merge: `missing-approval` is used by both a missing-approval case and the distinct `BUSINESS_SCOPE_ORDER_NOT_OWNED` case;
- seven semantic roots are fragmented across multiple `semanticTaskId` values;
- Task cross-split leakage is completely missed by the current `semanticTaskId` naming;
- several Governance validation controls deliberately use unique semanticTaskId strings despite matching held-out semantic roots.

Stage 1 therefore introduces a non-mutating audit overlay: `benchmark/v1/audit/stage1-semantic-root-map.json`.

## Review truth

Current resource facts:

```text
historical humanReviewed=true flags = 82
reviewer identities found            = 0
review timestamps found              = 0
review artifacts/records found       = 0
evidence-backed HUMAN_REVIEWED       = 0
```

Stage 1 does **not** rewrite the historical fields. Instead every current case is recorded in the audit overlay as `MODEL_REVIEWED`, because this Coding Agent performed the current audit. The old `humanReviewed=true` values are treated as provenance-uncertain legacy metadata, not evidence of human review.

## Gold provenance

Dedicated-case audit classification:

| Gold source | Cases |
|---|---:|
| SECURITY_POLICY_DERIVED | 33 |
| FAULT_CONTRACT_DERIVED | 19 |
| HAND_AUTHORED | 13 |
| DOMAIN_INVARIANT | 9 |
| LEGACY_MIGRATED | 8 |
| UNKNOWN | 0 |

No current repository code was found that directly runs the Agent and writes its output back into Gold. However, the 8 `LEGACY_MIGRATED` Task cases do not have reconstructable historical review/derivation artifacts, so their original provenance remains weaker than a frozen business-fixture-derived Gold process.

## Exact duplicates

Using benchmark-specific runtime payloads rather than only the superficial `input` field:

```text
raw exact runtime-payload pairs        = 0
normalized exact runtime-payload pairs = 0
exact input+key-Gold signature pairs = 0
normalized input+key-Gold signature pairs = 0
```

This does **not** mean the dataset is independent: semantic-root reuse is the dominant issue.

## Production/runtime audit

Current NL Task reachability was rechecked in `RuleBasedAgentTaskInterpreter`, `RulePlannerService` and workflow/tool code. The four dedicated Task scenarios remain reachable:

- `daily_review`
- `comment_risk`
- `product_optimization`
- `ad_anomaly`

No dedicated Task case claims `report_sync`, `refund_execute` or `product_update` as an NL task.

## Dataset mutation status

```text
New formal Benchmark cases = 0
Gold changes               = 0
Split moves                = 0
Formal manifest refreeze   = 0
Production runtime changes = 0
```

The Phase 6 dataset manifest is intentionally left unchanged. Stage 1 metadata lives in an audit overlay so the frozen Phase 6 case hashes are not silently invalidated.
