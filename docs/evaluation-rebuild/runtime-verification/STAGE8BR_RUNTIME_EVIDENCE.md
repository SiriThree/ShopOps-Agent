# Stage 8B-R Runtime Evidence

Contract A reached the write boundary for all 4 planned legal JDBC attempts with 0 pre-idempotency blocks.

Contract B consumed-approval counterexample passed in the Stage 6 Spring suite and remains attribution-ineligible.

Contract C payload conflict passed in the Stage 6 Spring suite and reached the idempotency boundary.

Contract D missing-effect detection passed in the Stage 6 evaluator suite.

Contract E concurrent first-write passed in JDBC/MySQL: one persisted `write_operation`, one effective external effect, duplicate effects 0.

Formal held-out metrics remain `NOT AVAILABLE`.
