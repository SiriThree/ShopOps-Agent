package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import org.junit.jupiter.api.Test;

class FormalBenchmarkEligibilityTest {
    @Test void taskCannotBecomeFormalFromPureCompilationOnly() {
        FormalRuntimeCapabilities c = new FormalRuntimeCapabilities();
        c.heldOutManifestVerified = true;
        FormalBenchmarkEligibility.Result r = new FormalBenchmarkEligibility().evaluate(BenchmarkType.TASK, c);
        assertThat(r.eligible()).isFalse();
        assertThat(r.missingGates()).contains("SPRING_RUNTIME_VERIFIED", "REAL_AGENT_RUNTIME");
    }

    @Test void governanceRequiresJdbcAuthorizationAndObjectOwnership() {
        FormalRuntimeCapabilities c = new FormalRuntimeCapabilities();
        c.springRuntimeVerified=true; c.heldOutManifestVerified=true; c.toolGatewayVerified=true;
        c.identityPropagationVerified=true; c.negativeControlsVerified=true; c.positiveControlsVerified=true;
        c.approvalPolicyVerified=true; c.writeOperationVerified=true; c.externalGroundTruthVerified=true;
        FormalBenchmarkEligibility.Result r = new FormalBenchmarkEligibility().evaluate(BenchmarkType.GOVERNANCE, c);
        assertThat(r.eligible()).isFalse();
        assertThat(r.missingGates()).contains("JDBC_AUTHORIZATION_VERIFIED", "BUSINESS_OBJECT_OWNERSHIP_VERIFIED");
    }

    @Test void idempotencyRequiresAttributionIsolationAndBoundaryReachability() {
        FormalRuntimeCapabilities c = new FormalRuntimeCapabilities();
        c.springRuntimeVerified=true; c.heldOutManifestVerified=true; c.toolGatewayVerified=true;
        c.writeOperationVerified=true; c.jdbcVerified=true; c.repeatedAttemptsVerified=true;
        c.externalGroundTruthVerified=true; c.nonIdempotentExternalVerified=true;
        FormalBenchmarkEligibility.Result r = new FormalBenchmarkEligibility().evaluate(BenchmarkType.IDEMPOTENCY, c);
        assertThat(r.eligible()).isFalse();
        assertThat(r.missingGates()).contains(
                "TRUSTED_IDENTITY_PROPAGATION",
                "JDBC_AUTHORIZATION_VERIFIED",
                "SCHEMA_VALIDATION_VERIFIED",
                "REAL_APPROVAL_POLICY",
                "BUSINESS_OBJECT_OWNERSHIP_VERIFIED",
                "ATTRIBUTION_ISOLATION_VERIFIED",
                "REPLAY_REACHED_IDEMPOTENCY_BOUNDARY",
                "MISSING_EFFECT_MEASURABLE");
    }
}
