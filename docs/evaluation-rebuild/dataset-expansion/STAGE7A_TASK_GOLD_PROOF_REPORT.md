# Stage 7A Task Gold Proof Report

Every newly admitted root had its fixture and expected business facts fixed before natural-language case generation.

- New roots: **64**
- New roots with Gold proof: **64**
- New roots without Gold proof: **0**
- `agentOutputUsed=true`: **0**
- `productionBusinessServiceUsed=true`: **0**
- Derivation: `stage7a-reference-oracle-v1(raw fixture only)`

Gold-source distribution for all 189 Task cases:
| Source | Cases |
|---|---|
| BUSINESS_FIXTURE_DERIVED | 168 |
| HAND_AUTHORED | 13 |
| LEGACY_MIGRATED | 8 |

The reference proof reads raw fixture rows and derives only the minimal facts required by the Gold contract; it does not run the Agent to create Gold.
