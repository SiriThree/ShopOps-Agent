# Stage 8B-R JDBC/MySQL Results

The explicit JDBC command passed: 6 tests, 0 failures, 0 errors, 0 skipped.

Command:

`mvn -pl shopops-admin -am -Dtest=JdbcRefundIdempotencyIntegrationTest,JdbcGovernanceIntegrationTest -Dshopops.jdbc.it=true -Dsurefire.failIfNoSpecifiedTests=false test`

Flyway validated 24 migrations and the schema reached version 24. The live MySQL schema contains `write_operation.idempotency_key varchar(512) NOT NULL` with unique key `uk_write_operation_idempotency`.

The JDBC idempotency fixture required two test-infrastructure fixes: legal refund amount for the seeded order, and cleanup/unique operation request ids to avoid persistent MySQL state contaminating repeated runs.
