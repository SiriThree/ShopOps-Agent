# Stage 3 Governance Review Report

## Candidate review

- proposed roots: 29
- accepted: 23
  - accepted positive: 17
  - accepted negative: 6
- rejected: 6

### Rejection taxonomy

| Rejection | Count |
|---|---:|
| `REJECTED_EVALUATOR_UNOBSERVABLE` | 2 |
| `REJECTED_NO_FIXTURE` | 2 |
| `REJECTED_UNSUPPORTED_RUNTIME` | 1 |
| `REJECTED_NOT_SEMANTICALLY_DISTINCT` | 1 |
| `REJECTED_GOLD_AMBIGUOUS` | 0 |
| `REJECTED_NEAR_DUPLICATE` | 0 |

The rejected candidates are preserved in the blueprint file with reason text. The most important rejected positives are product-title approved write and Feishu sync: both could look like useful legal traffic, but ShopOpsBench lacks an independent side-effect fact source for them, so adding them to a safety denominator would be weaker evidence.

## Author / Critic result

Each accepted root records why it is semantically distinct, the policy/fixture basis, split assignment, and Critic decision. Coding-agent review is classified as `MODEL_REVIEWED`, never human review.

A concrete Critic correction removed a fake Negative/Positive duplication for `ad.suggest_budget` pre-approval: identical valid requests with identical `REQUIRES_APPROVAL` outcomes cannot become two semantic roots merely by assigning different labels.

## Near-duplicate review

- candidate pairs: 555
- reviewed: 555
- same-root: 10
- distinct: 512
- distinct paired controls: 33
- unresolved: 0

`KEEP_DISTINCT_PAIRED_CONTROL` is used where attack and legitimate control are intentionally similar in business shape but differ in governance policy state and expected decision.

## Remaining review risks

- 33 historical Governance cases still contain `humanReviewed=true` without reviewer identity/timestamp/record; they remain historical uncertain flags.
- no Stage 3 case has evidence-backed human review.
- real formal False Reject behavior is not known until the candidate is frozen and executed through the formal runtime gate.
