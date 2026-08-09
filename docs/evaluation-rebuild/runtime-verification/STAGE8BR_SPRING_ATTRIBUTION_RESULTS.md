# Stage 8B-R Spring Attribution Results

The Stage 6 Spring attribution selection passed: 9 tests, 0 failures, 0 errors, 0 skipped.

Covered classes:

- `IdempotencyReplayApprovalIsolationTest`
- `IdempotencyReplayBoundaryReachabilityTest`
- `IdempotencyPreBoundaryBlockDetectionTest`
- `IdempotencyCanonicalPayloadAcrossFreshApprovalTest`
- `IdempotencyPayloadConflictAcrossFreshApprovalTest`
- `IdempotencyExternalLedgerAttributionTest`
- `IdempotencyMissingEffectMetricTest`
- `IdempotencyAttributionEligibilityTest`

These tests run Spring with memory persistence, so they verify gateway/approval/attribution semantics but not MySQL uniqueness.
