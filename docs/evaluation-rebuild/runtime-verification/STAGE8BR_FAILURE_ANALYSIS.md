# Stage 8B-R Failure Analysis

| Failure | Classification | Root cause | Fix | Rerun result |
|---|---|---|---|---|
| `McpToolDto` no-arg constructor missing | TEST_INFRASTRUCTURE_BUG | Test helper drifted after DTO constructor changed | Updated test to use current constructor | Compile passed |
| JDBC idempotency got business-scope block | TEST_INFRASTRUCTURE_BUG | Fixture used seeded order but invalid refund amount 1288 > 178.00 | Set refund amount to 4 | JDBC test passed |
| JDBC idempotency rerun polluted by old rows | TEST_INFRASTRUCTURE_BUG | Persistent MySQL reused task id / idempotency evidence from prior JVM | Clean benchmark task rows and use unique request id | Combined JDBC run passed |
| Full `mvn test` dataset/manifest failures | DATASET_CONTRACT_DEFECT | Existing dataset cases and manifest expectations are inconsistent with current expanded dataset | Not fixed in this stage because dataset edits are forbidden | Full regression still fails |
