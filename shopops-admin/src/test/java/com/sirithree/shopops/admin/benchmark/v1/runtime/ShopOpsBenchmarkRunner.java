package com.sirithree.shopops.admin.benchmark.v1.runtime;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.evidence.BenchmarkEvidenceCollector;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.BenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.sirithree.shopops.admin.benchmark.v1.metrics.TaskMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.metrics.IdempotencyMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyBenchmarkExecutor;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RecoveryBenchmarkExecutor;
import com.sirithree.shopops.admin.benchmark.v1.metrics.RecoveryMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.metrics.GovernanceMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.governance.GovernanceBenchmarkExecutor;

public class ShopOpsBenchmarkRunner {
    private final BenchmarkRuntimeGateway runtimeGateway;
    private final BenchmarkEvidenceCollector evidenceCollector;
    private final BenchmarkEvaluator evaluator;
    private final IdempotencyBenchmarkExecutor idempotencyExecutor;
    private final RecoveryBenchmarkExecutor recoveryExecutor;
    private final GovernanceBenchmarkExecutor governanceExecutor;

    public ShopOpsBenchmarkRunner(BenchmarkRuntimeGateway runtimeGateway,
                                  BenchmarkEvidenceCollector evidenceCollector,
                                  BenchmarkEvaluator evaluator) {
        this(runtimeGateway, evidenceCollector, evaluator, null, null, null);
    }

    public ShopOpsBenchmarkRunner(BenchmarkRuntimeGateway runtimeGateway,
                                  BenchmarkEvidenceCollector evidenceCollector,
                                  BenchmarkEvaluator evaluator,
                                  IdempotencyBenchmarkExecutor idempotencyExecutor) {
        this(runtimeGateway, evidenceCollector, evaluator, idempotencyExecutor, null, null);
    }

    public ShopOpsBenchmarkRunner(BenchmarkRuntimeGateway runtimeGateway,
                                  BenchmarkEvidenceCollector evidenceCollector,
                                  BenchmarkEvaluator evaluator,
                                  IdempotencyBenchmarkExecutor idempotencyExecutor,
                                  RecoveryBenchmarkExecutor recoveryExecutor) {
        this(runtimeGateway, evidenceCollector, evaluator, idempotencyExecutor, recoveryExecutor, null);
    }

    public ShopOpsBenchmarkRunner(BenchmarkRuntimeGateway runtimeGateway,
                                  BenchmarkEvidenceCollector evidenceCollector,
                                  BenchmarkEvaluator evaluator,
                                  IdempotencyBenchmarkExecutor idempotencyExecutor,
                                  RecoveryBenchmarkExecutor recoveryExecutor,
                                  GovernanceBenchmarkExecutor governanceExecutor) {
        this.runtimeGateway = runtimeGateway;
        this.evidenceCollector = evidenceCollector;
        this.evaluator = evaluator;
        this.idempotencyExecutor = idempotencyExecutor;
        this.recoveryExecutor = recoveryExecutor;
        this.governanceExecutor = governanceExecutor;
    }

    public EvaluationRun run(List<BenchmarkCase> cases,
                             BenchmarkRunRequest request,
                             EvaluationRunMetadata metadata) {
        EvaluationRun run = new EvaluationRun();
        run.metadata = metadata;
        List<BenchmarkCase> selected = filter(cases, request);
        for (BenchmarkCase benchmarkCase : selected) {
            run.caseExecutions.add(runCase(benchmarkCase, request, metadata));
        }
        metadata.finishedAt = Instant.now();
        run.aggregate = aggregate(run.caseExecutions);
        run.taskMetrics = new TaskMetricsAggregator().aggregate(run.caseExecutions);
        run.idempotencyMetrics = new IdempotencyMetricsAggregator().aggregate(run.caseExecutions);
        run.recoveryMetrics = new RecoveryMetricsAggregator().aggregate(run.caseExecutions);
        run.governanceMetrics = new GovernanceMetricsAggregator().aggregate(run.caseExecutions);
        return run;
    }

    public EvaluationRecord runCase(BenchmarkCase benchmarkCase,
                                    BenchmarkRunRequest request,
                                    EvaluationRunMetadata metadata) {
        EvaluationRecord record = baseRecord(benchmarkCase, metadata);
        if (benchmarkCase.benchmarkType == BenchmarkType.IDEMPOTENCY) {
            if (idempotencyExecutor == null) {
                record.executionStatus = CaseExecutionStatus.NOT_EXECUTED;
                record.failureReasons.add(FailureReasonCode.BENCHMARK_TYPE_NOT_IMPLEMENTED.name());
                return record;
            }
            return idempotencyExecutor.execute(benchmarkCase, request, metadata);
        }
        if (benchmarkCase.benchmarkType == BenchmarkType.RECOVERY) {
            if (recoveryExecutor == null) {
                record.executionStatus = CaseExecutionStatus.NOT_EXECUTED;
                record.failureReasons.add(FailureReasonCode.BENCHMARK_TYPE_NOT_IMPLEMENTED.name());
                return record;
            }
            return recoveryExecutor.execute(benchmarkCase, request, metadata);
        }
        if (benchmarkCase.benchmarkType == BenchmarkType.GOVERNANCE) {
            if (governanceExecutor == null) {
                record.executionStatus = CaseExecutionStatus.NOT_EXECUTED;
                record.failureReasons.add(FailureReasonCode.BENCHMARK_TYPE_NOT_IMPLEMENTED.name());
                return record;
            }
            return governanceExecutor.execute(benchmarkCase, request, metadata);
        }
        if (benchmarkCase.benchmarkType != BenchmarkType.TASK) {
            record.executionStatus = CaseExecutionStatus.NOT_EXECUTED;
            record.failureReasons.add(FailureReasonCode.BENCHMARK_TYPE_NOT_IMPLEMENTED.name());
            return record;
        }

        BenchmarkRuntimeRequest runtimeRequest = BenchmarkRuntimeRequest.from(benchmarkCase);
        BenchmarkRuntimeResult runtimeResult;
        try {
            runtimeResult = runtimeGateway.execute(runtimeRequest, request);
        } catch (RuntimeException ex) {
            record.executionStatus = CaseExecutionStatus.ERROR;
            record.failureReasons.add(FailureReasonCode.INFRASTRUCTURE_ERROR.name());
            record.observedFacts.put("runtimeError", safe(ex.getMessage()));
            return record;
        }

        record.executionStatus = runtimeResult.executionStatus;
        record.taskId = runtimeResult.taskId;
        record.finalState = runtimeResult.finalState;
        record.observedIntent.putAll(runtimeResult.observedInterpretation);
        record.observedPlan.putAll(runtimeResult.endpointPlanPreview);
        if (runtimeResult.runtimeFailureReason != null) {
            record.failureReasons.add(runtimeResult.runtimeFailureReason.name());
        }
        if (runtimeResult.executionStatus == CaseExecutionStatus.ERROR
                || runtimeResult.executionStatus == CaseExecutionStatus.NOT_EXECUTED
                || runtimeResult.taskId == null) {
            if (runtimeResult.runtimeFailureReason == null) {
                record.failureReasons.add(FailureReasonCode.INFRASTRUCTURE_ERROR.name());
            }
            record.observedFacts.put("runtimeError", runtimeResult.infrastructureError);
            return record;
        }

        try {
            CollectedEvidence evidence = evidenceCollector.collect(
                    longValue(runtimeRequest.identity.get("tenantId")),
                    longValue(runtimeRequest.identity.get("shopId")),
                    runtimeResult.taskId,
                    runtimeResult.traceId
            );
            record.observedFacts.putAll(evidence.businessFacts);
            record.observedPlan.put("actualPlannerObservation", evidence.plannerObservation);
            record.evidenceRefs.addAll(evidence.evidenceRefs);
            record.stateTransitions.addAll(evidence.taskEvents.stream().map(this::stateEvent).toList());
            record.agentSteps.addAll(evidence.steps.stream().map(this::agentStep).toList());
            record.toolAttempts.addAll(evidence.toolLogs.stream().map(this::compactToolLog).toList());
            record.toolResults.addAll(evidence.toolLogs.stream().map(this::compactToolResult).toList());
            record.approvalEvents.addAll(evidence.approvals.stream().map(this::approvalEvent).toList());
            record.writeOperations.addAll(evidence.writeOperations.stream().map(this::writeOperation).toList());
            record.sideEffects.addAll(evidence.sideEffects);
            record.faultEvents.addAll(evidence.faultEvents);
            record.businessOutcome.putAll(evidence.businessFacts);

            EvaluationResult evaluation = evaluator.evaluate(benchmarkCase, evidence);
            applyMetrics(record, benchmarkCase, evaluation);
            record.failureReasons.addAll(evaluation.failureReasons.stream().map(Enum::name).toList());
            record.failureReasons = record.failureReasons.stream().distinct().toList();
            record.executionStatus = evaluation.passed && record.failureReasons.isEmpty()
                    ? CaseExecutionStatus.PASSED : CaseExecutionStatus.FAILED;
            return record;
        } catch (RuntimeException ex) {
            record.executionStatus = CaseExecutionStatus.ERROR;
            record.failureReasons.add(FailureReasonCode.EVALUATION_ERROR.name());
            record.observedFacts.put("evaluationError", safe(ex.getMessage()));
            return record;
        }
    }

    private EvaluationRecord baseRecord(BenchmarkCase benchmarkCase, EvaluationRunMetadata metadata) {
        EvaluationRecord record = new EvaluationRecord();
        record.caseId = benchmarkCase.caseId;
        record.scenario = benchmarkCase.scenario;
        record.difficulty = benchmarkCase.difficulty;
        record.tags.addAll(benchmarkCase.tags == null ? java.util.List.of() : benchmarkCase.tags);
        record.semanticTaskId = benchmarkCase.semanticTaskId;
        record.origin = benchmarkCase.origin;
        record.evaluationRunId = metadata.runId;
        record.input.putAll(benchmarkCase.input == null ? Map.of() : benchmarkCase.input);
        record.runtimeMetadata.put("environment", metadata.environment);
        record.runtimeMetadata.put("executionLevel", metadata.executionLevel);
        record.runtimeMetadata.put("runtimeMode", metadata.runtimeMode);
        record.runtimeMetadata.put("interpreterMode", metadata.interpreterMode);
        record.runtimeMetadata.put("plannerMode", metadata.plannerMode);
        record.runtimeMetadata.put("datasetVersion", metadata.datasetVersion);
        record.runtimeMetadata.put("datasetSplit", metadata.datasetSplit);
        return record;
    }

    private void applyMetrics(EvaluationRecord record, BenchmarkCase benchmarkCase, EvaluationResult evaluation) {
        record.metricBreakdown.businessOutcomeCorrect = bool(evaluation, "businessOutcomeCorrect");
        record.metricBreakdown.toolExecutionValid = bool(evaluation, "toolExecutionValid");
        record.metricBreakdown.governanceSatisfied = bool(evaluation, "governanceSatisfied");
        record.metricBreakdown.noUnexpectedSideEffect = bool(evaluation, "noUnexpectedSideEffect");
        record.metricBreakdown.finalStateCorrect = bool(evaluation, "finalStateCorrect");
        record.metricBreakdown.taskSuccess = bool(evaluation, "taskSuccess");
        record.metricBreakdown.redundantToolCallCount = intValue(evaluation.metricValues.get("redundantToolCallCount"));
        record.metricBreakdown.optionalToolFailureCount = intValue(evaluation.metricValues.get("optionalToolFailureCount"));
        record.metricBreakdown.toolValidationFailureCount = intValue(evaluation.metricValues.get("toolValidationFailureCount"));
        record.metricBreakdown.incorrectSuccess = Boolean.TRUE.equals(record.metricBreakdown.businessOutcomeCorrect) ? false : isRuntimeSuccess(record.finalState);
        Object planner = record.observedPlan.get("actualPlannerObservation");
        if (planner instanceof Map<?, ?> plannerMap) {
            Object fallback = plannerMap.get("fallback");
            record.metricBreakdown.plannerFallback = fallback instanceof Boolean b ? b : null;
        }
        record.metricBreakdown.expectedLogicalSideEffects = benchmarkCase.sideEffectExpectation == null
                ? 0 : benchmarkCase.sideEffectExpectation.expectedLogicalSideEffects;
        record.metricBreakdown.actualEffectiveSideEffects = null;
        record.metricBreakdown.duplicateSideEffects = null;
        record.metricBreakdown.unauthorizedWriteCount = intValue(evaluation.metricValues.get("unauthorizedWriteCount"));
        record.metricBreakdown.approvalBypassCount = intValue(evaluation.metricValues.get("approvalBypassCount"));
        record.metricBreakdown.crossTenantViolationCount = intValue(evaluation.metricValues.get("crossTenantViolationCount"));
    }


    private boolean isRuntimeSuccess(String state) {
        if (state == null) return false;
        return "SUCCESS".equalsIgnoreCase(state) || "SUCCEEDED".equalsIgnoreCase(state);
    }

    private Boolean bool(EvaluationResult evaluation, String key) {
        Object value = evaluation.metricValues.get(key);
        return value instanceof Boolean bool ? bool : null;
    }

    private Integer intValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private List<BenchmarkCase> filter(List<BenchmarkCase> cases, BenchmarkRunRequest request) {
        if (cases == null) return List.of();
        List<BenchmarkCase> result = new ArrayList<>();
        for (BenchmarkCase benchmarkCase : cases) {
            if (request.benchmarkType != null && benchmarkCase.benchmarkType != request.benchmarkType) continue;
            if (request.caseId != null && !request.caseId.equals(benchmarkCase.caseId)) continue;
            if (request.scenario != null && !request.scenario.equals(benchmarkCase.scenario)) continue;
            if (request.tag != null && (benchmarkCase.tags == null || !benchmarkCase.tags.contains(request.tag))) continue;
            result.add(benchmarkCase);
        }
        return result;
    }

    private AggregateReport aggregate(List<EvaluationRecord> records) {
        AggregateReport aggregate = new AggregateReport();
        aggregate.totalCases = records.size();
        for (EvaluationRecord record : records) {
            switch (record.executionStatus) {
                case PASSED -> {
                    aggregate.executedCases++;
                    aggregate.passedCases++;
                }
                case FAILED -> {
                    aggregate.executedCases++;
                    aggregate.failedCases++;
                }
                case EXECUTED -> aggregate.executedCases++;
                case NOT_EXECUTED -> aggregate.notExecutedCases++;
                case ERROR -> {
                    aggregate.notExecutedCases++;
                    if (record.failureReasons.contains(FailureReasonCode.INFRASTRUCTURE_ERROR.name())) {
                        aggregate.infrastructureErrors++;
                    }
                }
            }
            for (String reason : record.failureReasons) {
                aggregate.failureReasons.merge(reason, 1, Integer::sum);
            }
        }
        return aggregate;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> compactToolLog(Map<String, Object> log) {
        Map<String, Object> compact = new java.util.LinkedHashMap<>();
        for (String key : List.of("id", "taskId", "stepId", "traceId", "toolCode", "status", "riskLevel", "approvalId", "retryCount", "createdAt")) {
            if (log.containsKey(key)) compact.put(key, log.get(key));
        }
        if (log.containsKey("input")) compact.put("inputHash", sha256(String.valueOf(log.get("input"))));
        return compact;
    }

    private Map<String, Object> compactToolResult(Map<String, Object> log) {
        Map<String, Object> compact = new java.util.LinkedHashMap<>();
        for (String key : List.of("id", "toolCode", "status", "errorCode", "errorMessage", "latencyMs", "retryCount")) {
            if (log.containsKey(key)) compact.put(key, log.get(key));
        }
        if (log.containsKey("output")) compact.put("outputHash", sha256(String.valueOf(log.get("output"))));
        return compact;
    }

    private Map<String, Object> agentStep(com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto step) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("stepId", step.getStepId());
        map.put("stepNo", step.getStepNo());
        map.put("stepName", step.getStepName());
        map.put("toolCode", step.getToolCode());
        map.put("status", step.getStatus());
        if (step.getErrorMessage() != null) map.put("errorMessage", safe(step.getErrorMessage()));
        if (step.getInput() != null) map.put("inputHash", sha256(String.valueOf(step.getInput())));
        if (step.getOutput() != null) map.put("outputHash", sha256(String.valueOf(step.getOutput())));
        return map;
    }

    private Map<String, Object> stateEvent(com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto event) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("eventId", event.getEventId());
        map.put("eventType", event.getEventType());
        map.put("fromStatus", event.getFromStatus());
        map.put("toStatus", event.getToStatus());
        map.put("operatorId", event.getOperatorId());
        map.put("createdAt", event.getCreatedAt());
        return map;
    }

    private Map<String, Object> approvalEvent(com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto approval) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("approvalId", approval.getApprovalId());
        map.put("toolCode", approval.getToolCode());
        map.put("status", approval.getStatus());
        map.put("riskLevel", approval.getRiskLevel());
        map.put("requesterId", approval.getRequesterId());
        map.put("approverId", approval.getApproverId());
        map.put("stepId", approval.getStepId());
        map.put("sourceId", approval.getSourceId());
        map.put("createdAt", approval.getCreatedAt());
        map.put("decidedAt", approval.getDecidedAt());
        if (approval.getInputSummary() != null) {
            map.put("inputSummaryHash", sha256(approval.getInputSummary()));
        }
        return map;
    }

    private Map<String, Object> writeOperation(com.sirithree.shopops.admin.reliability.domain.WriteOperation operation) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("writeOperationId", operation.getId());
        map.put("operationRequestId", operation.getOperationRequestId());
        map.put("idempotencyKey", operation.getIdempotencyKey());
        map.put("toolCode", operation.getToolCode());
        map.put("businessObjectId", operation.getBusinessObjectId());
        map.put("approvalId", operation.getApprovalId());
        map.put("status", operation.getStatus());
        map.put("externalReference", operation.getExternalReference());
        map.put("lastErrorCode", operation.getLastErrorCode());
        map.put("retryAction", operation.getRetryAction());
        map.put("version", operation.getVersion());
        map.put("freshExecution", operation.isFreshExecution());
        map.put("createdAt", operation.getCreatedAt());
        map.put("updatedAt", operation.getUpdatedAt());
        return map;
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String safe(String value) {
        if (value == null) return "unavailable";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
