# Stage 8B-R Concurrency Results

`JdbcRefundIdempotencyIntegrationTest` ran 4 concurrent callers with 4 fresh approvals against real MySQL/JDBC.

Observed database state after the run:

- `write_operation` rows for benchmark task range: 1
- distinct idempotency keys: 1
- final write status: `SUCCEEDED`
- effective effects asserted by test: 1
- duplicate effects asserted by test: 0

This verifies the available concurrent first-write contract for the current fixture.
