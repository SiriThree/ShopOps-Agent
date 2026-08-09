# Stage 4 Handoff

## What changed

Recovery dataset only.

```text
Production runtime changes    NONE
Recovery evaluator changes    NONE
Task dataset changes          NONE
Governance dataset changes    NONE
Idempotency dataset changes   NONE
```

## Current Recovery candidate

```text
Cases                         21
Causal roots                  15
dev                            7 / 4 roots
validation                     8 / 5 roots
test                           6 / 6 roots

True test-exclusive roots      6
Cross-split root leakage       0
Cross-split parent leakage     0
Unresolved near duplicates     0
```

## Candidate generation

```text
Proposed  17
Accepted   8
Rejected   9
```

Important rejected gaps:

- final external `FAILED` causal sequence: external-truth fixture unavailable;
- final external `UNKNOWN`: independent final truth unavailable;
- stale scan: no controllable stale-clock fixture;
- stop at `EXTERNAL_SUCCEEDED` / `LOCAL_CONFIRMED`: fault point unavailable;
- deterministic post-query CAS/version conflict: fault point/fixture unavailable;
- complete correlation loss: current DB/provider contract does not expose this state.

## Current causal coverage

```text
External:
SUCCEEDED      11 roots
NOT_ACCEPTED    4 roots
FAILED          0
UNKNOWN         0

Initial:
EXECUTING        5 roots
EXTERNAL_UNKNOWN 10 roots

Budget:
EARLY_SUCCESS          11
LAST_ALLOWED_SUCCESS    1
BUDGET_EXHAUSTED        3

Manual review    3 roots
Concurrency      3 roots
```

## Important dataset correction

Seven Phase 6 held-out recovery cases had stale `sideEffectExpectation.businessTarget` values that no longer matched their real-seed `input.orderId`. Stage 4 corrects these under the new candidate Gold version.

## Candidate manifest

`shopops-admin/src/test/resources/benchmark/v1/benchmark-recovery-stage4-candidate-manifest.json`

```text
datasetVersion = 1.4.0-stage4-recovery-candidate
goldVersion    = shopopsbench-gold-v1.4-recovery-stage4
status         = EXPANSION_CANDIDATE
formalRunOccurred = false
manifestSha256 = 775da3f194e2815004a21cfa72aff70a3cd768ff7c52c97dc868ca588b7d2a01
```

The Phase 6 frozen manifest is retained byte-for-byte as historical baseline; it is not overwritten by the expansion candidate.

## Next-stage recommendation

Proceed to Idempotency group-split repair and semantic-scenario expansion rather than adding another round of Recovery cases.

Recovery now has 15 roots and six genuinely held-out roots. Its remaining gaps (`FAILED`, final `UNKNOWN`, stale scan and precise CAS checkpoint) require testability/evidence-contract work rather than more JSON generation. Idempotency still had zero truly test-exclusive roots in the Stage 1 audit, making it the higher-priority dataset-independence problem.

Do **not** run the Stage 4 held-out Recovery roots as development feedback before the next formal freeze.
