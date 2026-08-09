# ShopOpsBench v1.1 — Task Dataset Card

## Version

- Benchmark contract: ShopOpsBench v1
- Phase 2 dataset version: `1.1.0-phase2-task`
- Gold version: `shopopsbench-gold-v1.1`

## Dataset size

Phase 2 Task cases in the versioned dev/validation/test splits:

```text
Unique Semantic Tasks       16
Natural Language Variants   21
TASK Benchmark Cases        21
Execution Runs               0 in this environment
```

Execution runs are intentionally not multiplied into “unique task” count.

## Split distribution

| Split | TASK cases | Role |
|---|---:|---|
| dev | 9 | evaluator/runner/debug development |
| validation | 6 | regression and design validation |
| test | 6 | held-out formal-test candidate |
| **Total** | **21** | |

The repository also retains 4 non-TASK Phase 0 contract cases across these splits for future Idempotency/Recovery/Governance work. They are not counted in the 21 Task cases.

## Business category distribution

| Scenario | Cases |
|---|---:|
| `daily_review` | 7 |
| `comment_risk` | 6 |
| `product_optimization` | 4 |
| `ad_anomaly` | 4 |
| **Total** | **21** |

`REPORT_SYNC` is intentionally absent because `feishu.sync_report` is not reachable from the current natural-language Agent planner.

## Difficulty distribution

| Difficulty | Cases |
|---|---:|
| EASY | 4 |
| MEDIUM | 13 |
| HARD | 4 |

## Tag distribution

The Phase 2 TASK dataset currently contains:

```text
TASK                       21
READ_ONLY                  21
MULTI_TOOL                 21
CLEAN                       9
MISSING_PARAMETER           7
NATURAL_LANGUAGE_VARIANT    5
AMBIGUOUS                    2
HELD_OUT                     6
```

There are no write/approval TASK cases because current NL planning does not reach the write Tools.

## Provenance distribution

```text
LEGACY          4
HAND_AUTHORED  12
PERTURBED       5
PUBLIC_DATA_DERIVED 0
```

All 21 Phase 2 TASK cases are marked `humanReviewed=true`.

## Legacy 280 migration audit

The old natural-language batch is generated from exactly four Prompt templates:

1. daily review;
2. comment risk;
3. product optimization;
4. ad anomaly.

The old script iterates dates and rounds. Under the commonly cited 7-day × 10-round run this yields:

```text
4 templates × 7 dates × 10 rounds = 280 executions
```

That is execution repetition, not 280 unique semantic tasks.

Phase 2 treatment:

| Legacy element | Decision | Reason |
|---|---|---|
| Four business templates | REWRITE / MIGRATE | useful seeds for the four actually reachable NL task families |
| Date substitutions | AUGMENT only when business facts differ | dates are parameters, not automatically new semantics |
| Ten repeated rounds | KEEP as future repeated execution runs | useful for model/runtime stability, not dataset diversity |
| Legacy PASS condition | DROP for E2E Task Success | intent + task terminal + report/evidence existence is insufficient |
| Legacy Tool trace | KEEP as regression evidence | not the new Gold definition |

Four Phase 2 cases carry `origin=LEGACY`; they are reviewed rewrites, not blind copies of 280 executions.

The legacy script also contains stale metric lookups (`adPerformance.cost` while report evidence uses `spend`, and `externalReports` while current report evidence uses `externalReportMetrics`), another reason not to promote its historical CSV directly into new Gold.

## Perturbation quality

The five `PERTURBED` cases use manual structural paraphrases rather than superficial “please/help me” changes. They retain:

- `parentCaseId`;
- `perturbationType`;
- `generationMethod`;
- `semanticTaskId`.

## Duplicate detection

`DatasetQualityValidator` implements low-complexity checks for:

- exact duplicate input;
- normalized duplicate input;
- same semantic parent with Jaccard similarity >= 0.90.

Actual static Phase 2 validation result:

```text
NEAR_DUPLICATE_CHECK PASS
issues = 0
```

No ML duplicate detector was introduced.

## Held-out protection

The test split has six TASK cases tagged `HELD_OUT`.

Protection mechanisms:

1. `BenchmarkRuntimeRequest` contains only runtime-visible input/identity/initial state fields and has no Gold fields.
2. `scripts/run-shopops-benchmark.ps1 -Split test` is blocked unless `-FormalTest` is explicitly supplied.
3. production/evaluator code contains no case-ID switch logic.
4. test cases are not used to define a unique fixed Tool trace.

This is engineering isolation, not cryptographic secrecy: repository maintainers can still open the JSON file.

## Known limitations

- Rule-based interpreter remains the primary NL router.
- All current formal TASK cases are read-oriented analysis/report tasks.
- Multi-turn clarification is not supported.
- Schema-error injection through the current NL planner is not supported because planner output does not own Tool arguments.
- No formal test execution occurred in the current environment because Maven is unavailable.
