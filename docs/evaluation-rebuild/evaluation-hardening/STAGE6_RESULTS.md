# Stage 6 Results

## Scope

Stage 6 is Evaluation Contract Hardening, not dataset expansion.

Dataset remains:

- 22 dedicated Idempotency cases;
- 16 semantic scenarios;
- 4 test-exclusive scenarios;
- 0 cross-split Idempotency root leakage.

No split or business Gold changed.

## Implemented

1. Fresh approval per intended replay.
2. Per-attempt attribution evidence.
3. Pre-idempotency block taxonomy.
4. `IdempotencyAttributionEligibility`.
5. Evaluator attribution-invalid gate.
6. Existing duplicate/missing-effect formulas retained and explicitly surfaced.
7. Formal eligibility upgraded with attribution isolation plus trusted identity/JDBC authorization/schema/approval/scope prerequisite gates.
8. Workload attribution overlay added without changing Stage 5 planned workload size.
9. Eight Stage 6 contract tests added.
10. Static audit + isolated Java compile + pure attribution harness added.

## Integrity

Stage 5 baseline comparison:

- Production `src/main`: 530 files, tree hash unchanged.
- Idempotency case files: unchanged byte-for-byte.
- Stage 5 candidate manifest: unchanged.
- Stage 5 workload profile: unchanged.
- Other Task/Governance/Recovery benchmark resource tree: unchanged.
- New semantic scenarios: 0.
- Split changes: 0.
- Gold changes: 0.

## Evaluation changes

Changed benchmark/test infrastructure includes:

- `RefundIdempotencyBenchmarkExecutor`
- `FreshReplayApprovalFactory`
- `IdempotencyAttemptAttribution`
- `IdempotencyAttributionClassifier`
- `IdempotencyAttributionEligibility`
- `CollectedEvidence`
- `EvaluationRecord`
- `SideEffectIdempotencyEvaluator`
- Idempotency metrics aggregation
- Formal Idempotency eligibility facts/gates
- Stage 6 contract tests

No production code was changed.

## Executed evidence

### PASS

- Stage 6 deterministic static audit.
- Isolated `javac` compilation of modified driver/evaluator dependency set.
- Isolated `javac` compilation of Formal eligibility gate.
- Pure Java attribution classifier/eligibility harness.
- Dataset byte integrity.
- Production tree integrity.
- Benchmark hack scan.
- Held-out root/case reference scan in Stage 6 contract tests.

### NOT RUN

- Maven/JUnit Stage 6 Spring contract tests.
- JDBC/MySQL runtime.
- Stage 5 test-exclusive Idempotency scenarios.
- Formal workload.
- Formal Idempotency metrics.

## Attribution quality gate

| Gate | Status |
|---|---|
| Fresh approval replay implementation/static contract | PASS |
| Canonical same-payload hash static contract | PASS |
| Payload conflict hash/static contract | PASS |
| Boundary evidence model | PASS |
| Pre-boundary block classifier | PASS |
| External attempt/effect separation | PASS |
| Missing effect formula | PASS |
| Formal attribution eligibility gate | PASS |
| Production integrity | PASS |
| Fresh approval real Spring replay test | NOT RUN |
| Real Spring boundary reachability test | NOT RUN |
| Real Spring consumed-approval counterexample | NOT RUN |

Because the runtime contract tests are NOT RUN, Stage 6 is implemented but not yet runtime-verified for freeze.

## Formal status

`Formal Idempotency Metric = NOT AVAILABLE`.

No held-out Formal execution occurred.
