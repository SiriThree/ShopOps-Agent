# Stage 1 Expansion Priorities

The purpose of this document is to identify missing information, not to back-solve a target case count.

## P0 — repair semantic split independence before any formal score

**Evidence**

- 25 dedicated semantic roots cross split.
- only 6 of 31 held-out roots are test-exclusive;
- Idempotency: 0/6 test roots exclusive;
- Recovery: 1/7;
- Governance: 2/12;
- Task: 3/6.

**Action**

Before the next formal freeze, assign entire root families to exactly one split. Do not cherry-pick only failed/poor cases. Version-bump the affected datasets and regenerate the manifest after the migration.

## P0 — Task held-out semantic diversity

**Current evidence**

```text
21 cases / 12 roots
6 held-out cases
3 test-exclusive roots
all 21 are MULTI_TOOL
0 dedicated EMPTY_RESULT
0 PARTIAL_DATA
0 TOOL_FAILURE
0 DEGRADED
0 DATE_BOUNDARY
```

**Recommended next expansion**

Add roughly **40–50 new Task semantic roots** in the first high-quality expansion wave, not 179 mechanically calculated cases. Aim for approximately **80–100 new Task cases** after adding only useful within-root language variants.

Priority root families:

- real empty-data states by domain;
- partial-source states only where runtime semantics are defined;
- required-tool failure/degraded behavior;
- date-boundary and safe-default cases;
- normal/no-risk ad state, not only RISK_FOUND;
- low/high business-data density;
- domain-specific goal emphasis that changes outcome requirements rather than merely wording.

Gold should be fixture/invariant-derived wherever possible. No new test root may share a dev/validation root.

## P0 — Governance positive controls / False Reject denominator

**Current evidence**

```text
25 negative cases / 18 negative roots
8 positive cases / 5 positive roots
held-out positives = 4 cases
test-exclusive positive roots = 1
```

A high Unauthorized Block Rate with this denominator would still provide weak evidence that legitimate traffic is preserved.

**Recommended future expansion**

Add **10–14 positive semantic roots** before emphasizing more malicious permutations. Prioritize symmetric controls for cross-shop/business-scope/permission/schema/approval boundaries. Add roughly **6–10 negative roots** only where they test a genuinely different enforcement boundary.

## P1 — Idempotency held-out redesign and workload scale

**Current evidence**

```text
15 cases / 9 semantic scenarios
6 held-out roots / 0 test-exclusive
15 configured logical operations
37 configured attempts
```

First re-split roots. Then add approximately **4–6 genuinely new semantic scenarios** only if production boundaries support them. Separately create formal workload plans with **200–300 logical operations** distributed across scenarios; workload repetitions are not new semantic roots.

## P1 — Recovery causal diversity

**Current evidence**

```text
13 cases / 7 causal roots
7 held-out roots / 1 test-exclusive
11/13 start with External=SUCCEEDED
12/13 use recovery budget 3
```

After re-splitting, add approximately **5–7 new causal roots** around stale intermediate operations, external failure, correlation loss/unavailability, recovery-update/CAS conflict, and broader initial states. Do not create seed-only variants as roots.

## P2 — fixture and wording breadth

Once semantic coverage and split independence are repaired:

- broaden Task dates/business states where fixture evidence is real;
- add natural-language variants within already assigned root groups;
- avoid all-task shop-1 concentration if real fixtures support additional shops;
- perform human review or clearly retain `MODEL_REVIEWED`.

## Review strategy

Stage 1 found **0 evidence-backed HUMAN_REVIEWED cases**. The next generation stage should preserve:

```text
UNREVIEWED
MODEL_REVIEWED
HUMAN_REVIEWED
HUMAN_SPOT_CHECKED
```

A model author + model critic workflow remains `MODEL_REVIEWED`. Human status requires a review artifact with reviewer and timestamp.
