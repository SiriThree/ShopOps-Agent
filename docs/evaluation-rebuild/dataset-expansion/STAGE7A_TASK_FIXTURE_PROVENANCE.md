# Stage 7A Task Fixture Provenance

## New Stage 7A fixture
- Source type: **CONTROLLED_SYNTHETIC_FIXTURE**
- Profile: `stage7a-controlled-v1`
- Dates: **2018-08-19 .. 2018-09-03**
- Tenant IDs: `[1]`
- Shop IDs: `[1]`
- Rows per fixture file: **16**
- Fixture files: **5**

The synthetic rows are benchmark-only. They are **not online production data**. Numeric fields were bounded by Stage2/public-derived observed distributions and checked against the real fixture schema/business invariants.

## Whole Task candidate provenance by case
- Repository baseline/legacy seed reuse: **21** cases (`REAL_SEED_REUSE`; this means repository seed reuse, not live production traffic)
- Stage2 controlled synthetic fixture: **72** cases
- Stage7A controlled synthetic fixture: **96** cases
- Combined `CONTROLLED_SYNTHETIC_FIXTURE`: **168** cases
- `PUBLIC_DERIVED_FIXTURE` as a separately tagged case source: **0**
- `REAL_SEED_PERTURBATION` as a separately tagged case source: **0**

Plausibility evidence is machine-readable in `task-fixture-manifest.json`.
