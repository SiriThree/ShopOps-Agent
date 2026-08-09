# Stage 7B Governance Dataset Card

Status: `EXPANSION_CANDIDATE`; Formal Governance NOT RUN; new held-out roots not executed.

Cases **106**, roots **96**, positive roots **47**, negative roots **49**, test-exclusive **54** (28 positive / 26 negative).

| Family | Cases | Roots | Test roots |
|---|---:|---:|---:|
| Identity | 13 | 11 | 4 |
| Permission | 34 | 32 | 18 |
| Approval | 20 | 16 | 8 |
| Schema | 30 | 29 | 17 |
| Capability | 2 | 1 | 0 |
| Business Object Scope | 3 | 3 | 3 |
| Economic Boundary | 4 | 4 | 4 |

| Positive control | Cases | Roots | Test roots |
|---|---:|---:|---:|
| LEGITIMATE_READ | 20 | 19 | 9 |
| LEGITIMATE_MEDIUM_RISK | 6 | 6 | 3 |
| LEGITIMATE_HIGH_RISK_PREAPPROVAL | 18 | 17 | 12 |
| LEGITIMATE_APPROVED_WRITE | 6 | 5 | 4 |
| LEGITIMATE_SCHEMA_BOUNDARY | 9 | 9 | 5 |
| LEGITIMATE_BUSINESS_SCOPE | 1 | 1 | 1 |
| LEGITIMATE_ECONOMIC_BOUNDARY | 3 | 3 | 3 |
| LEGITIMATE_PERMISSION_SNAPSHOT | 3 | 3 | 3 |
| LEGITIMATE_IDEMPOTENT_REPLAY | 1 | 1 | 1 |
| LEGITIMATE_MCP_READ | 6 | 5 | 3 |

Limitations: capability diversity is constrained by real fixtures; multi-shop/multi-tenant business-object fixtures remain limited; evidence-backed human review is 0; no runtime rate is claimed.
