# Stage 7A Task Dataset Card

## Candidate identity
- Dataset version: `1.3.0-stage7a-task-scaleup-candidate`
- Gold version: `shopopsbench-gold-v1.3-task-stage7a`
- Status: `EXPANSION_CANDIDATE`
- Formal run: **false**
- Held-out execution: **false**

## Size
- Cases: **189**
- Semantic roots: **116**
- True test-exclusive roots: **63**
- Case/root ratio: **1.629**

## Split
| Split | Cases | Roots |
|---|---|---|
| dev | 45 | 27 |
| validation | 41 | 26 |
| test | 103 | 63 |

## Business domains
| Scenario | Cases | Roots | Test-exclusive roots |
|---|---|---|---|
| daily_review | 49 | 29 | 16 |
| comment_risk | 48 | 28 | 16 |
| product_optimization | 48 | 30 | 16 |
| ad_anomaly | 44 | 29 | 15 |

## Business-state coverage
| Tag | Cases | Roots |
|---|---|---|
| CLEAN | 86 | 60 |
| EMPTY_RESULT | 33 | 20 |
| PARTIAL_DATA | 34 | 20 |
| DATE_BOUNDARY | 20 | 12 |
| MISSING_PARAMETER | 7 | 4 |
| AMBIGUOUS | 2 | 1 |
| LOW_DATA_DENSITY | 57 | 35 |
| MEDIUM_DATA_DENSITY | 48 | 32 |
| HIGH_DATA_DENSITY | 43 | 26 |

## Language
| Language | Cases |
|---|---|
| Chinese | 170 |
| English | 19 |

## Fixture scope
Tenant IDs remain `[1]` and shop IDs remain `[1]`. Stage7A adds deterministic dates 2018-08-19..2018-09-03; tenant/shop diversity remains a limitation rather than a fabricated source of semantic roots.
