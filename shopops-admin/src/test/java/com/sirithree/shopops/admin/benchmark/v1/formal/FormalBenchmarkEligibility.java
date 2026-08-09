package com.sirithree.shopops.admin.benchmark.v1.formal;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import java.util.ArrayList;
import java.util.List;

/** Prevents a test/pure run from being promoted to a formal ShopOpsBench metric. */
public class FormalBenchmarkEligibility {
    public Result evaluate(BenchmarkType type, FormalRuntimeCapabilities c) {
        List<String> missing = new ArrayList<>();
        require(c.springRuntimeVerified, "SPRING_RUNTIME_VERIFIED", missing);
        require(c.heldOutManifestVerified, "HELD_OUT_MANIFEST_VERIFIED", missing);
        switch (type) {
            case TASK -> {
                require(c.taskRunnerVerified, "REAL_BENCHMARK_RUNNER", missing);
                require(c.agentRuntimeVerified, "REAL_AGENT_RUNTIME", missing);
                require(c.toolGatewayVerified, "REAL_TOOL_GATEWAY", missing);
                require(c.evidenceCollectorVerified, "REAL_EVIDENCE_COLLECTOR", missing);
                require(c.businessOutcomeEvaluatorVerified, "BUSINESS_OUTCOME_EVALUATOR", missing);
                require(c.goldLeakageVerified, "NO_GOLD_LEAKAGE", missing);
            }
            case IDEMPOTENCY -> {
                require(c.toolGatewayVerified, "REAL_TOOL_GATEWAY", missing);
                require(c.identityPropagationVerified, "TRUSTED_IDENTITY_PROPAGATION", missing);
                require(c.authorizationJdbcVerified, "JDBC_AUTHORIZATION_VERIFIED", missing);
                require(c.schemaValidationVerified, "SCHEMA_VALIDATION_VERIFIED", missing);
                require(c.approvalPolicyVerified, "REAL_APPROVAL_POLICY", missing);
                require(c.businessObjectOwnershipVerified, "BUSINESS_OBJECT_OWNERSHIP_VERIFIED", missing);
                require(c.writeOperationVerified, "REAL_WRITE_OPERATION", missing);
                require(c.jdbcVerified, "JDBC_MYSQL_VERIFIED", missing);
                require(c.repeatedAttemptsVerified, "REPEATED_ATTEMPTS_REACHED_PRODUCTION", missing);
                require(c.externalGroundTruthVerified, "INDEPENDENT_EXTERNAL_GROUND_TRUTH", missing);
                require(c.nonIdempotentExternalVerified, "NON_IDEMPOTENT_EXTERNAL_MODE", missing);
                require(c.attributionIsolationVerified, "ATTRIBUTION_ISOLATION_VERIFIED", missing);
                require(c.replayBoundaryReachabilityVerified, "REPLAY_REACHED_IDEMPOTENCY_BOUNDARY", missing);
                require(c.missingEffectMeasurableVerified, "MISSING_EFFECT_MEASURABLE", missing);
            }
            case RECOVERY -> {
                require(c.productionRecoveryVerified, "PRODUCTION_RECONCILIATION", missing);
                require(c.jdbcVerified, "JDBC_MYSQL_VERIFIED", missing);
                require(c.externalGroundTruthVerified, "INDEPENDENT_EXTERNAL_GROUND_TRUTH", missing);
                require(c.productionFaultInjectionVerified, "PRODUCTION_BOUNDARY_FAULT_INJECTION", missing);
                require(c.boundedRecoveryVerified, "BOUNDED_RECOVERY", missing);
                require(c.benchmarkSideRepairAbsentVerified, "NO_BENCHMARK_SIDE_STATE_REPAIR", missing);
                require(c.duplicateEffectCheckVerified, "DUPLICATE_EFFECT_CHECKED", missing);
            }
            case GOVERNANCE -> {
                require(c.toolGatewayVerified, "REAL_TOOL_GATEWAY", missing);
                require(c.identityPropagationVerified, "TRUSTED_IDENTITY_PROPAGATION", missing);
                require(c.authorizationJdbcVerified, "JDBC_AUTHORIZATION_VERIFIED", missing);
                require(c.negativeControlsVerified, "NEGATIVE_CASES_EXECUTED", missing);
                require(c.positiveControlsVerified, "POSITIVE_CONTROLS_EXECUTED", missing);
                require(c.approvalPolicyVerified, "REAL_APPROVAL_POLICY", missing);
                require(c.writeOperationVerified, "REAL_WRITE_OPERATION", missing);
                require(c.externalGroundTruthVerified, "INDEPENDENT_EXTERNAL_GROUND_TRUTH", missing);
                require(c.businessObjectOwnershipVerified, "BUSINESS_OBJECT_OWNERSHIP_VERIFIED", missing);
            }
        }
        return new Result(missing.isEmpty(), List.copyOf(missing));
    }

    private void require(boolean ok, String gate, List<String> missing) {
        if (!ok) missing.add(gate);
    }

    public record Result(boolean eligible, List<String> missingGates) {}
}
