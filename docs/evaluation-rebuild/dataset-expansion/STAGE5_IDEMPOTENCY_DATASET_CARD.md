# Stage 5 Idempotency Dataset Card

## Candidate scope

Stage 5 modifies Idempotency benchmark resources only. Production runtime is unchanged and held-out formal execution did not occur.

## Size

- Cases: **22**
- Semantic scenarios: **16**
- dev: **11 cases / 6 roots**
- validation: **7 / 6**
- test: **4 / 4**
- true test-exclusive roots: **4**

## Payload relation coverage

- SAME_KEY + SAME_PAYLOAD: **10 roots**
- SAME_KEY + DIFFERENT_BUSINESS_PAYLOAD: **6 roots**
- SAME_KEY + SAME_BUSINESS + DIFFERENT_METADATA: **0 accepted roots**
- DIFFERENT_KEY + SAME_BUSINESS_TARGET: **0 accepted roots**

## Gold / review

- DOMAIN_INVARIANT: **9 cases**
- FAULT_CONTRACT_DERIVED: **13 cases**
- Unknown Gold: **0**
- MODEL_REVIEWED: **22**
- evidence-backed HUMAN_REVIEWED: **0**
- historical `humanReviewed=true`: **15**, retained as uncertain legacy metadata

## External truth

21 cases use `NON_IDEMPOTENT_EXTERNAL`; one historical `IDEMPOTENT_EXTERNAL` comparison is a control and cannot prove application-layer duplicate prevention.
