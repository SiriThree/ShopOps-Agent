# Stage 6 Handoff

## What Stage 6 fixed

The Idempotency benchmark no longer intentionally reuses one consumed approval for all repeated deliveries.

Every intended replay now has a fresh valid approval while preserving the same logical operation identity and semantic payload. Benchmark evidence can distinguish:

- governance/precondition block before idempotency;
- actual idempotency-boundary decision.

## What remains unchanged

- Production runtime: unchanged.
- Stage 5 Dataset: 22 cases / 16 scenarios / 4 test-exclusive scenarios.
- Splits: unchanged.
- Gold: unchanged.
- Stage 5 planned Formal workload: 260 logical operations / 700 planned attempts.
- Stage 5 candidate manifest: unchanged.

## Current evidence

PASS:

- static attribution audit;
- isolated Java compilation of driver/evaluator dependency set;
- pure attribution harness;
- production/dataset integrity;
- no benchmark-specific production behavior.

NOT RUN:

- Stage 6 Spring/JUnit contract tests;
- JDBC/MySQL;
- held-out Formal Idempotency;
- planned Formal workload.

## Known remaining blockers

1. Maven and Maven Wrapper unavailable in the current environment.
2. Spring contract tests have not executed the fresh-approval lifecycle.
3. JDBC/MySQL concurrency/unique-key/winner-reread behavior remains unverified.
4. Stage 5 different-key/same-target business semantics remain undefined.
5. Per-attempt timeout-before -> later-accept transition still lacks a deterministic driver contract.
6. Refund still has no Rabbit consumer; no refund MQ redelivery claim is allowed.
7. Existing Phase-6 Formal Idempotency integration wiring remains historical and should not be used as a new Stage-5/6 candidate freeze without Stage 7 manifest work.

## Freeze recommendation

Do **not** perform Global Candidate Freeze yet in the current environment.

First run the Stage 6 contract-test suite on a Maven-capable environment. Required results:

- fresh approval replay: PASS;
- trusted identity / authorization / schema / scope / approval prerequisites: PASS;
- same-payload approval binding: PASS;
- payload conflict reaches idempotency boundary: PASS;
- reused consumed approval classified as attribution-invalid: PASS;
- external attempt/effect separation: PASS;
- missing-effect evaluator counterexample: PASS;
- attribution eligibility: PASS.

If these pass without production changes, the next stage can be:

`Global Candidate Freeze -> Human Review Pack -> Formal Runtime Verification`.

If any contract test fails, fix the evaluation/test infrastructure first; do not change Stage 5 held-out Gold to match observed behavior.
