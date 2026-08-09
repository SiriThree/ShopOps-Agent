# Stage 2 Task Root Blueprints

## Root-first generation contract

Every proposed root was specified before natural-language variants using:

- semanticRootId
- businessScenario
- businessGoal
- fixtureFacts
- difficultyDimensions
- expectedOutcomeContract
- goldSource
- requiredCapabilities
- allowedAlternativeBehavior
- unsupportedBehavior
- plannedSplit

The machine-readable source is `benchmark/v1/task/stage2/task-root-blueprints.json`.

## Candidate result

- Proposed roots: **50**
- Accepted roots: **40**
- Rejected roots: **10**

Accepted by business domain:

- daily_review: 9
- comment_risk: 9
- product_optimization: 11
- ad_anomaly: 11

The 40 accepted roots generated 72 cases; variants never leave their root's assigned split.

## Rejections

### REJECTED_EVALUATOR_UNOBSERVABLE — 8

The existing evaluator cannot reliably judge the intended correct business behavior without evaluator changes, which Stage 2 forbids.

Rejected examples include:

- daily_review with valid ad `NO_DATA` partial-source semantics: current daily evaluator requires ad metric maps.
- comment_risk where risk comments exist but candidate products are empty: current evaluator requires an affected-product intersection.
- required/optional tool failure and degraded-success candidates: current outcome evaluators require successful structured tool outputs and have no supported degraded-success branch.

These are recorded as `EVALUATOR_COVERAGE_GAP`; no evaluator was modified to admit them.

### REJECTED_GOLD_AMBIGUOUS — 2

`today` / `yesterday` roots were rejected because the current benchmark has no controlled Clock. Their Gold would drift with wall-clock time.

## Fixture feasibility

Accepted roots use the Stage 2 controlled test-resource profile built from the existing public-derived 2018-08-01..07 baseline plus realistic benchmark-only states for 2018-08-08..18. Production data stores are not mutated.

All accepted roots satisfy:

1. Natural-language runtime family is reachable.
2. Fixture state exists.
3. Gold is deterministic before Agent execution.
4. Existing evaluator can observe the required facts.
5. No new production behavior is required.
