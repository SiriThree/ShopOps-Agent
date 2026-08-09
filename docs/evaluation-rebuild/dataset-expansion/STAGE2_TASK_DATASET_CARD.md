# Stage 2 Task Dataset Card

## Status

- Status: `EXPANSION_CANDIDATE`
- Formal run occurred: `false`
- Dataset version: `1.2.0-stage2-task-candidate`
- Gold version: `shopopsbench-gold-v1.2-task-stage2`
- Dedicated cases: 93
- Semantic roots: 52
- New cases: 72
- New roots: 40

## Split distribution

- dev: 27 cases / 15 roots
- validation: 23 cases / 14 roots
- test: 43 cases / 23 roots
- test-exclusive roots: 23

## Business domain distribution

- daily_review: 25 cases / 13 roots / 6 test roots
- comment_risk: 24 / 12 / 6
- product_optimization: 24 / 14 / 6
- ad_anomaly: 20 / 13 / 5

## Difficulty

- EASY: 4
- MEDIUM: 61
- HARD: 28

## Language

Based on `input.userInput`:

- Chinese: 84
- English: 9

Chinese remains the dominant language, matching the intended operations-user setting; English is retained as limited expression-robustness coverage.

## Fixture distribution

- tenant: 1 for 93 / 93 cases
- shop: 1 for 93 / 93 cases
- explicit/date-scoped fixture coverage: 2018-08-01 through 2018-08-18, plus safe-default cases without explicit date
- Stage 2 controlled fixture states: 2018-08-08 through 2018-08-18

This is a real limitation: tenant/shop variation is not counted as semantic diversity, and Stage 2 does not invent tenants merely to make the distribution look wider.

## Business-state coverage

New controlled states include legitimate empty comments/candidates, ad NO_DATA/NORMAL/RISK_FOUND, low/high density, partial commerce state, and a fixed date boundary. No wall-clock-relative `today`/`yesterday` Gold was accepted.

## Gold sources

- BUSINESS_FIXTURE_DERIVED: 72
- HAND_AUTHORED: 13
- LEGACY_MIGRATED: 8
- UNKNOWN: 0

No Task Gold requires an exact `expectedToolCodes` sequence.

## Review truth

- reviewStatus MODEL_REVIEWED: 93
- new Stage 2 humanReviewed=false: 72
- evidence-backed HUMAN_REVIEWED: 0
- historical `humanReviewed=true`: 21, retained as historical uncertain flags rather than reinterpreted as evidence-backed review

## Root/case ratio

93 cases / 52 roots = 1.79 cases per semantic root. This is reported only as a density descriptor; roots, not cases, are the unit of semantic independence.
