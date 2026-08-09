# Stage 6 Formal Idempotency Eligibility

## New attribution gates

`FormalBenchmarkEligibility` now requires the following additional Idempotency facts:

- `ATTRIBUTION_ISOLATION_VERIFIED`
- `REPLAY_REACHED_IDEMPOTENCY_BOUNDARY`
- `MISSING_EFFECT_MEASURABLE`

Existing and prerequisite gates remain required:

- Spring runtime;
- held-out manifest;
- real Tool Gateway;
- trusted identity propagation;
- JDBC authorization;
- schema validation;
- real approval policy;
- business-object ownership/scope;
- real WriteOperation;
- JDBC/MySQL;
- repeated attempts reaching production;
- independent external ground truth;
- non-idempotent external mode.

## Eligibility rule

A duplicate-side-effect result cannot become a Formal application-idempotency metric when:

- authorization stopped the replay;
- schema stopped the replay;
- business scope stopped the replay;
- approval stopped the replay;
- the first legitimate operation never reached the boundary;
- external effective effects cannot be measured;
- missing effects cannot be measured.

In those cases the formal metric remains `NOT AVAILABLE`.

## Current status

Stage 6 implementation adds the required test infrastructure and gates.

Current environment:

- Maven: NOT FOUND;
- Maven Wrapper: ABSENT;
- Docker: NOT FOUND;
- Spring/JUnit Stage 6 contract tests: NOT RUN;
- JDBC/MySQL Formal Idempotency: NOT RUN;
- Stage 5 held-out scenarios: NOT RUN.

Therefore:

`Formal Idempotency Metric = NOT AVAILABLE`

and

`Ready for Global Candidate Freeze = NO (runtime contract verification still required)`.
