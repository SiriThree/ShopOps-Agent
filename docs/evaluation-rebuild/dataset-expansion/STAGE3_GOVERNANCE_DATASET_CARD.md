# Stage 3 Governance Dataset Card

## Identity

- benchmark: `GOVERNANCE`
- status: `EXPANSION_CANDIDATE`
- dataset version: `1.5.0-stage3-governance-candidate`
- Gold version: `shopopsbench-gold-v1.5-governance-stage3`
- execution level represented by the dataset: primarily `TOOL_GATEWAY`
- formal run occurred: **false**

## Size and roots

- cases: **56**
- semantic roots: **46**
- dev: 15 cases / 11 roots
- validation: 23 cases / 17 roots
- test: 18 cases / 18 roots
- all test roots are test-exclusive

## Class balance

- negative: 31 cases / 24 roots
- positive: 25 cases / 22 roots
- held-out negative roots: 6
- held-out positive roots: 12

## Negative attack-family coverage

| Family | Cases | Roots | Test roots |
|---|---:|---:|---:|
| Identity | 7 | 5 | 1 |
| Permission | 5 | 4 | 1 |
| Approval | 8 | 6 | 1 |
| Schema | 7 | 6 | 1 |
| Capability / unknown tool | 2 | 1 | **0** |
| Business object/economic scope | 2 | 2 | 2 |

Capability has no test-exclusive negative root: a second arbitrary unknown-tool alias was rejected as not semantically distinct, while deterministic disabled-tool fixture support is absent.

## Positive-control breadth

The final dataset includes these real control classes (categories can overlap):

- legitimate read roots: 6 (2 held-out)
- legitimate high-risk pre-approval roots: 10 (6 held-out)
- legitimate approved-write roots: 2 (1 held-out)
- legitimate business-scope roots: 2 (2 held-out)
- legitimate schema-boundary roots: 4 (2 held-out)
- legitimate permission-snapshot roots: 1 (1 held-out)
- legitimate idempotent-replay roots: 1 (1 held-out)
- legitimate MCP-read roots: 1 (currently validation, not test-exclusive)

## Gold provenance

All 56 current dedicated Governance cases are mapped to `SECURITY_POLICY_DERIVED`. No `UNKNOWN` Gold source exists in the candidate.

Gold is fixed from governance policy/catalog/schema/approval/business-scope contracts before formal execution. Stage 3 does not use Tool Gateway output to rewrite Gold.

## Review truth

- `MODEL_REVIEWED`: 56 cases
- evidence-backed `HUMAN_REVIEWED`: 0
- historical `humanReviewed=true` flags: 33, retained as historical uncertain metadata
- new Stage 3 cases: 23; all `humanReviewed=false`

## Authorization/fixture scope

Dataset construction uses deterministic authorization fixture metadata grounded in the real VIEWER/OPERATOR/ADMIN permission mapping. It does not claim JDBC RBAC was executed during Stage 3.

Business-object controls use the established owned-order fixture `SO202607180001` and its refundable boundary contract. Stage 3 itself does not run held-out Tool Gateway integrations.
