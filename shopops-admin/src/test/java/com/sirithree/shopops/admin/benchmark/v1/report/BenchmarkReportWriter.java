package com.sirithree.shopops.admin.benchmark.v1.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRun;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class BenchmarkReportWriter {
    private final ObjectMapper objectMapper;

    public BenchmarkReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReportPaths write(EvaluationRun run, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        String runId = run.metadata == null || run.metadata.runId == null ? "unidentified-run" : run.metadata.runId;
        Path json = outputDir.resolve(runId + ".json");
        Path markdown = outputDir.resolve(runId + ".md");
        Files.writeString(json,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(run),
                StandardCharsets.UTF_8);
        Files.writeString(markdown, markdown(run), StandardCharsets.UTF_8);
        return new ReportPaths(json, markdown);
    }

    public String markdown(EvaluationRun run) {
        StringBuilder out = new StringBuilder();
        out.append("# ShopOpsBench Run\n\n");
        out.append("> Dataset classification: **")
                .append(run.metadata == null ? "UNAVAILABLE" : run.metadata.datasetSplit)
                .append(" / ")
                .append(run.metadata == null ? "UNAVAILABLE" : run.metadata.environment)
                .append("**. Smoke/dev runs are not formal benchmark scores.\n\n");
        if (run.metadata != null) {
            out.append("- Run ID: `").append(run.metadata.runId).append("`\n");
            out.append("- Benchmark: ").append(run.metadata.benchmarkVersion).append("\n");
            out.append("- Dataset: ").append(run.metadata.datasetVersion).append(" / ").append(run.metadata.datasetSplit).append("\n");
            out.append("- Environment: ").append(run.metadata.environment).append("\n");
            out.append("- Execution level: ").append(run.metadata.executionLevel).append("\n");
            out.append("- Runtime mode: ").append(run.metadata.runtimeMode).append("\n");
            out.append("- Interpreter: ").append(run.metadata.interpreterMode).append("\n");
            out.append("- Planner: ").append(run.metadata.plannerMode).append("\n");
            out.append("- Database: ").append(run.metadata.databaseMode).append("\n");
            out.append("- Queue: ").append(run.metadata.queueMode).append("\n");
            out.append("- External system: ").append(run.metadata.externalSystemMode).append("\n");
            out.append("- Authorization mode: ").append(run.metadata.authorizationMode).append("\n");
            out.append("- Fault seed: ").append(run.metadata.faultSeed).append("\n");
        }
        out.append("\n## Cases\n\n");
        out.append("- Total: ").append(run.aggregate.totalCases).append("\n");
        out.append("- Executed: ").append(run.aggregate.executedCases).append("\n");
        out.append("- Passed: ").append(run.aggregate.passedCases).append("\n");
        out.append("- Failed: ").append(run.aggregate.failedCases).append("\n");
        out.append("- Not executed: ").append(run.aggregate.notExecutedCases).append("\n");
        out.append("- Infrastructure errors: ").append(run.aggregate.infrastructureErrors).append("\n");

        boolean formalTest = run.metadata != null && "test".equalsIgnoreCase(run.metadata.datasetSplit);
        boolean idempotencyOnly = run.idempotencyMetrics != null
                && run.idempotencyMetrics.executedCases > 0
                && (run.taskMetrics == null || run.taskMetrics.executedCases == 0)
                && (run.recoveryMetrics == null || run.recoveryMetrics.executedCases == 0)
                && (run.governanceMetrics == null || (run.governanceMetrics.unauthorizedCasesExecuted == 0 && run.governanceMetrics.legitimateCasesExecuted == 0));
        boolean recoveryOnly = run.recoveryMetrics != null
                && run.recoveryMetrics.executedCases > 0
                && (run.taskMetrics == null || run.taskMetrics.executedCases == 0)
                && (run.idempotencyMetrics == null || run.idempotencyMetrics.executedCases == 0)
                && (run.governanceMetrics == null || (run.governanceMetrics.unauthorizedCasesExecuted == 0 && run.governanceMetrics.legitimateCasesExecuted == 0));
        boolean governanceOnly = run.governanceMetrics != null
                && (run.governanceMetrics.unauthorizedCasesExecuted > 0 || run.governanceMetrics.legitimateCasesExecuted > 0)
                && (run.taskMetrics == null || run.taskMetrics.executedCases == 0)
                && (run.idempotencyMetrics == null || run.idempotencyMetrics.executedCases == 0)
                && (run.recoveryMetrics == null || run.recoveryMetrics.executedCases == 0);
        if (!idempotencyOnly && !recoveryOnly && !governanceOnly) {
            out.append(formalTest ? "\n## End-to-End Agent Task Success (FORMAL_TEST)\n\n"
                    : "\n## Task Success Diagnostic (NON-FORMAL)\n\n");
            if (!formalTest) {
                out.append("> This run is not the held-out formal test split and must not be reported as the final End-to-End Agent Task Success.\n\n");
            }
            if (run.taskMetrics == null || run.taskMetrics.executedCases == 0) {
                out.append("- Task Success: NOT AVAILABLE (no valid executed cases)\n");
            } else {
                out.append("- Overall: ").append(run.taskMetrics.successCases).append(" / ")
                        .append(run.taskMetrics.executedCases).append(" = ")
                        .append(String.format(java.util.Locale.ROOT, "%.2f%%", run.taskMetrics.taskSuccessRate() * 100.0)).append("\n");
                out.append("- Incorrect Success: ").append(run.taskMetrics.incorrectSuccessCount).append(" / ")
                        .append(run.taskMetrics.executedCases).append("\n");
                out.append("- Model plan accepted: ").append(run.taskMetrics.modelPlanAcceptedCount).append("\n");
                out.append("- Model fallback: ").append(run.taskMetrics.modelFallbackCount).append("\n");
                if (run.taskMetrics.plannerFallbackRate() != null) {
                    out.append("- Model fallback rate: ")
                            .append(String.format(java.util.Locale.ROOT, "%.2f%%", run.taskMetrics.plannerFallbackRate() * 100.0)).append("\n");
                }
                out.append("- Rule based: ").append(run.taskMetrics.ruleBasedCount).append("\n");
            }

            out.append("\n### By Scenario\n\n");
            if (run.taskMetrics != null) run.taskMetrics.byScenario.forEach((key, slice) ->
                    out.append("- ").append(key).append(": ").append(slice.success).append(" / ").append(slice.executed)
                            .append(slice.rate() == null ? "" : " = " + String.format(java.util.Locale.ROOT, "%.2f%%", slice.rate() * 100.0)).append("\n"));

            out.append("\n### By Task Property\n\n");
            if (run.taskMetrics != null) run.taskMetrics.byTag.forEach((key, slice) ->
                    out.append("- ").append(key).append(": ").append(slice.success).append(" / ").append(slice.executed)
                            .append(slice.rate() == null ? "" : " = " + String.format(java.util.Locale.ROOT, "%.2f%%", slice.rate() * 100.0)).append("\n"));
        }

        if (run.idempotencyMetrics != null && run.idempotencyMetrics.executedCases > 0) {
            out.append("\n## Side-Effect Idempotency Raw Counts\n\n");
            out.append("> A `test` split alone does not make these counts formal; Phase 3 runtime-gate conditions still apply.\n\n");
            out.append("- Logical Write Requests: ").append(run.idempotencyMetrics.logicalWriteRequests).append("\n");
            out.append("- Delivery Attempts: ").append(run.idempotencyMetrics.deliveryAttempts).append("\n");
            out.append("- Execution Attempts: ").append(run.idempotencyMetrics.executionAttempts).append("\n");
            out.append("- Tool Attempts: ").append(run.idempotencyMetrics.toolAttempts).append("\n");
            out.append("- External Attempts: ").append(run.idempotencyMetrics.externalAttempts).append("\n");
            out.append("- Expected Effective Side Effects: ").append(run.idempotencyMetrics.expectedEffectiveSideEffects).append("\n");
            out.append("- Actual Effective Side Effects: ").append(run.idempotencyMetrics.actualEffectiveSideEffects).append("\n");
            out.append("- Duplicate Side Effects: ").append(run.idempotencyMetrics.duplicateSideEffects).append("\n");
            out.append("- Missing Side Effects: ").append(run.idempotencyMetrics.missingSideEffects).append("\n");
            if (run.idempotencyMetrics.duplicateSideEffectRate() != null) {
                out.append("- Duplicate Side Effect Rate: ")
                        .append(String.format(java.util.Locale.ROOT, "%.2f%%", run.idempotencyMetrics.duplicateSideEffectRate() * 100.0)).append("\n");
            }
            out.append("\n> These counts are only formal when the run metadata and Phase 3 gate prove production write execution, independent external ground truth, repeated attempts reaching the production boundary, and the required JDBC/queue level.\n");
        }


        if (run.recoveryMetrics != null && run.recoveryMetrics.executedCases > 0) {
            boolean formalRecovery = formalTest && run.metadata != null
                    && (run.metadata.environment == com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment.JDBC_INTEGRATION
                        || run.metadata.environment == com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment.EXTERNAL_REAL)
                    && run.metadata.databaseMode != null
                    && run.metadata.databaseMode.toLowerCase(java.util.Locale.ROOT).contains("jdbc");
            out.append("\n## State Convergence / Recovery Raw Counts\n\n");
            out.append("> Formal convergence requires production recovery execution, independent external truth, real fault injection, final local-state observation, no benchmark-side repair, duplicate-effect verification, and the required JDBC runtime gate.\n\n");
            if (!formalRecovery) out.append("> This run is NON-FORMAL recovery evidence; rates below are diagnostic only and must not be reported as the formal State Convergence Rate.\n\n");
            out.append("- Fault Cases: ").append(run.recoveryMetrics.faultCases).append("\n");
            out.append("- Executed: ").append(run.recoveryMetrics.executedCases).append("\n");
            out.append("- Terminal Reached: ").append(run.recoveryMetrics.terminalReached).append("\n");
            out.append("- State Correct: ").append(run.recoveryMetrics.stateCorrect).append("\n");
            out.append("- Converged: ").append(run.recoveryMetrics.converged).append("\n");
            out.append("- Permanent Stuck: ").append(run.recoveryMetrics.permanentStuck).append("\n");
            out.append("- Incorrect Terminal State: ").append(run.recoveryMetrics.incorrectTerminalState).append("\n");
            out.append("- Manual Review: ").append(run.recoveryMetrics.manualReview).append("\n");
            out.append("- Duplicate Side Effects: ").append(run.recoveryMetrics.duplicateSideEffects).append("\n");
            out.append("- Recovery Attempts: ").append(run.recoveryMetrics.totalRecoveryAttempts).append("\n");
            String ratePrefix = formalRecovery ? "" : "Diagnostic ";
            if (run.recoveryMetrics.terminalConvergenceRate != null) out.append("- ").append(ratePrefix).append("Terminal Convergence Rate: ").append(String.format(java.util.Locale.ROOT, "%.2f%%", run.recoveryMetrics.terminalConvergenceRate * 100.0)).append(formalRecovery ? "" : " (NON-FORMAL)").append("\n");
            if (run.recoveryMetrics.stateCorrectnessRate != null) out.append("- ").append(ratePrefix).append("State Correctness Rate: ").append(String.format(java.util.Locale.ROOT, "%.2f%%", run.recoveryMetrics.stateCorrectnessRate * 100.0)).append(formalRecovery ? "" : " (NON-FORMAL)").append("\n");
            if (run.recoveryMetrics.permanentStuckRate != null) out.append("- ").append(ratePrefix).append("Permanent Stuck Rate: ").append(String.format(java.util.Locale.ROOT, "%.2f%%", run.recoveryMetrics.permanentStuckRate * 100.0)).append(formalRecovery ? "" : " (NON-FORMAL)").append("\n");
            if (run.recoveryMetrics.automaticRecoveryRate != null) out.append("- ").append(ratePrefix).append("Automatic Recovery Rate: ").append(String.format(java.util.Locale.ROOT, "%.2f%%", run.recoveryMetrics.automaticRecoveryRate * 100.0)).append(formalRecovery ? "" : " (NON-FORMAL)").append("\n");
        }

        if (run.governanceMetrics != null
                && (run.governanceMetrics.unauthorizedCasesExecuted > 0 || run.governanceMetrics.legitimateCasesExecuted > 0)) {
            boolean formalGovernance = formalTest && run.metadata != null
                    && (run.metadata.executionLevel == com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkExecutionLevel.TOOL_GATEWAY
                        || run.metadata.executionLevel == com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkExecutionLevel.AGENT)
                    && (run.metadata.environment == com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment.JDBC_INTEGRATION
                        || run.metadata.environment == com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment.EXTERNAL_REAL)
                    && run.metadata.authorizationMode != null
                    && run.metadata.authorizationMode.toUpperCase(java.util.Locale.ROOT).contains("JDBC")
                    && run.metadata.databaseMode != null
                    && run.metadata.databaseMode.toLowerCase(java.util.Locale.ROOT).contains("jdbc");
            out.append("\n## Execution Governance Raw Counts\n\n");
            out.append("> Formal governance requires the real Tool Gateway, trusted authorization path, actual negative and positive controls, approval/risk enforcement, and independent external-side-effect ground truth.\n\n");
            if (!formalGovernance) out.append("> This run is NON-FORMAL governance evidence; rates below are diagnostic only and must not be reported as formal Governance rates.\n\n");
            out.append("- Unauthorized Cases Executed: ").append(run.governanceMetrics.unauthorizedCasesExecuted).append("\n");
            out.append("- Correctly Blocked Unauthorized: ").append(run.governanceMetrics.correctlyBlockedUnauthorizedCases).append("\n");
            out.append("- Legitimate Cases Executed: ").append(run.governanceMetrics.legitimateCasesExecuted).append("\n");
            out.append("- False Rejected Legitimate: ").append(run.governanceMetrics.falseRejectedLegitimateCases).append("\n");
            out.append("- Unauthorized Writes: ").append(run.governanceMetrics.unauthorizedWriteCount).append("\n");
            out.append("- Approval Bypass: ").append(run.governanceMetrics.approvalBypassCount).append("\n");
            out.append("- Cross-Tenant Violations: ").append(run.governanceMetrics.crossTenantViolationCount).append("\n");
            out.append("- Cross-Shop Violations: ").append(run.governanceMetrics.crossShopViolationCount).append("\n");
            String label = formalGovernance ? "" : "Diagnostic ";
            if (run.governanceMetrics.unauthorizedBlockRate() != null) out.append("- ").append(label).append("Unauthorized Block Rate: ")
                    .append(run.governanceMetrics.correctlyBlockedUnauthorizedCases).append(" / ")
                    .append(run.governanceMetrics.unauthorizedCasesExecuted).append(" = ")
                    .append(String.format(java.util.Locale.ROOT, "%.2f%%", run.governanceMetrics.unauthorizedBlockRate() * 100.0))
                    .append(formalGovernance ? "" : " (NON-FORMAL)").append("\n");
            if (run.governanceMetrics.falseRejectRate() != null) out.append("- ").append(label).append("False Reject Rate: ")
                    .append(run.governanceMetrics.falseRejectedLegitimateCases).append(" / ")
                    .append(run.governanceMetrics.legitimateCasesExecuted).append(" = ")
                    .append(String.format(java.util.Locale.ROOT, "%.2f%%", run.governanceMetrics.falseRejectRate() * 100.0))
                    .append(formalGovernance ? "" : " (NON-FORMAL)").append("\n");
            out.append("\n### By Attack Type\n\n");
            run.governanceMetrics.byAttackType.forEach((attack, slice) -> out.append("- ").append(attack)
                    .append(": correct ").append(slice.correct).append(" / ").append(slice.executed)
                    .append(", critical violations=").append(slice.violations).append("\n"));
        }

        out.append("\n## Failure Reasons\n\n");
        if (run.aggregate.failureReasons.isEmpty()) {
            out.append("- None\n");
        } else {
            new TreeMap<>(run.aggregate.failureReasons)
                    .forEach((reason, count) -> out.append("- ").append(reason).append(": ").append(count).append("\n"));
        }

        out.append("\n## Case Results\n\n");
        out.append("| caseId | status | taskId | finalState | governanceDecision | unauthorizedBlocked | falseRejected | approvalBypass | crossTenant | crossShop | taskSuccess | converged | recoveryAttempts | duplicateEffects | missingEffects | reasons |\n");
        out.append("|---|---|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for (EvaluationRecord record : run.caseExecutions) {
            out.append("|").append(escape(record.caseId))
                    .append("|").append(record.executionStatus)
                    .append("|").append(record.taskId == null ? "" : record.taskId)
                    .append("|").append(escape(record.finalState))
                    .append("|").append(escape(record.governanceDecision))
                    .append("|").append(record.metricBreakdown.unauthorizedBlocked)
                    .append("|").append(record.metricBreakdown.falseRejected)
                    .append("|").append(record.metricBreakdown.approvalBypassCount)
                    .append("|").append(record.metricBreakdown.crossTenantViolationCount)
                    .append("|").append(record.metricBreakdown.crossShopViolationCount)
                    .append("|").append(record.metricBreakdown.taskSuccess)
                    .append("|").append(record.metricBreakdown.converged)
                    .append("|").append(record.metricBreakdown.recoveryAttempts)
                    .append("|").append(record.metricBreakdown.duplicateSideEffects)
                    .append("|").append(record.metricBreakdown.missingSideEffects)
                    .append("|").append(escape(String.join(",", record.failureReasons)))
                    .append("|\n");
        }
        return out.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("|", "\\|").replace("\n", " ");
    }

    public record ReportPaths(Path json, Path markdown) {}
}
