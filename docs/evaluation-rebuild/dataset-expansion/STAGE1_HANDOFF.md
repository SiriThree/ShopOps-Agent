# Stage 1 Handoff

## Current raw cases

```text
All versioned case objects = 89
Dedicated cases            = 82
Contract examples           = 4
Smoke cases                 = 3
```

Dedicated split:

```text
Task          9 / 6 / 6   dev / validation / test
Idempotency   5 / 4 / 6
Recovery      3 / 3 / 7
Governance    9 / 12 / 12
```

## Current semantic roots

```text
Task          12
Idempotency    9
Recovery       7
Governance    23
Total         51
```

Held-out roots = 31, but only 6 roots are test-exclusive.

## Cross-split leakage

```text
Task           3 leaked roots
Idempotency    6
Recovery       6
Governance    10
Total         25
```

Cross-split same-root near-duplicate pairs: 27.

Cross-split parentCaseId leakage: 0.

## Near duplicates

```text
exact runtime payload pairs        0
normalized runtime payload pairs   0
exact input+key-Gold pairs         0
normalized input+key-Gold pairs    0
near-duplicate candidates         33
```

Candidates are not auto-deleted; same-root variants must be group-split together.

## Review-status truth

```text
historical humanReviewed=true flags = 82
evidence-backed HUMAN_REVIEWED       = 0
Stage 1 MODEL_REVIEWED               = 89
```

The original flags were not rewritten.

## Gold provenance

```text
SECURITY_POLICY_DERIVED   33
FAULT_CONTRACT_DERIVED    19
HAND_AUTHORED             13
DOMAIN_INVARIANT           9
LEGACY_MIGRATED            8
UNKNOWN                    0
```

Direct current-code self-reference evidence: 0.

Historical provenance of the 8 legacy-migrated cases is not independently reconstructable.

## Task gaps

- only 12 roots;
- only 3 test-exclusive roots;
- no dedicated empty-result, partial-data, tool-failure/degraded or date-boundary roots;
- all 21 cases are multi-tool;
- all cases use shop 1;
- ad_anomaly has only 2 roots and over-focuses risk-found behavior.

## Governance gaps

- 18 negative roots versus only 5 positive roots;
- held-out positive denominator = 4 cases;
- only 1 positive test root is test-exclusive;
- attack/control symmetry is incomplete.

## Recovery gaps

- 7 causal roots;
- only 1 held-out root is test-exclusive;
- external-success state dominates;
- stale/correlation-loss/external-failed/CAS-conflict semantic families are weak or absent.

## Idempotency gaps

- 9 semantic scenarios;
- 0 held-out roots are exclusive;
- only 15 configured logical writes / 37 attempts;
- semantic coverage and workload scale must be reported separately.

## Expansion priority

```text
P0 split independence migration
P0 Task semantic diversity
P0 Governance positive-control denominator
P1 Idempotency re-split + workload scale
P1 Recovery new causal roots
P2 fixture/language breadth after the above
```

## Recommended Stage 2

Do **not** bulk-generate 200 cases.

Start with **Task Benchmark**, because it is the primary end-to-end Agent metric and currently has the largest business/language/outcome-coverage deficiency.

Recommended first wave:

```text
+40 to +50 new Task semantic roots
+80 to +100 Task cases total from those roots/variants
```

The generation prompt must allocate roots to splits **before** writing variants, and every root family must remain in one split. Development should use dev/validation only; new held-out roots must not be executed to tune evaluator behavior.

After this first Task wave, rerun Stage 1 audit before deciding whether another Task wave is needed.

## Production runtime

UNCHANGED.
