# Stage 8B-R Build Results

Initial `mvn clean test` reached real Maven/JDK17 compilation and failed in test compilation. The cause was a test-infrastructure drift: `JdbcRefundOrderBusinessScopeValidatorTest` still used a removed no-arg `McpToolDto` constructor.

After the test-only fix, full `mvn test` executed 335 tests and failed with 12 failures, 20 errors, 21 skipped. The remaining failures are existing dataset/manifest contract failures, not runtime environment failures.
