package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evidence.EvidenceRef;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;

/**
 * Stage-6 Tool-Gateway driver. It does not deduplicate requests before production. Every intended replay receives a
 * fresh, normally-bound approval so governance prerequisites cannot masquerade as application idempotency. The
 * independently recording external transport remains the authority for effective side effects.
 */
public class RefundIdempotencyBenchmarkExecutor implements IdempotencyBenchmarkExecutor {
    private static final AtomicLong TASK_IDS = new AtomicLong(930000);
    private final ToolGatewayService toolGateway;
    private final ApprovalRequestService approvals;
    private final ToolCallLogService toolLogs;
    private final WriteOperationService writeOperations;
    private final RecordingRefundExternalSystem externalSystem;
    private final DeterministicReliabilityFaultController faults;
    private final SideEffectIdempotencyEvaluator evaluator = new SideEffectIdempotencyEvaluator();
    private final FreshReplayApprovalFactory replayApprovals;

    public RefundIdempotencyBenchmarkExecutor(ToolGatewayService toolGateway,
                                              ApprovalRequestService approvals,
                                              ToolCallLogService toolLogs,
                                              WriteOperationService writeOperations,
                                              RecordingRefundExternalSystem externalSystem,
                                              DeterministicReliabilityFaultController faults) {
        this.toolGateway = toolGateway;
        this.approvals = approvals;
        this.toolLogs = toolLogs;
        this.writeOperations = writeOperations;
        this.externalSystem = externalSystem;
        this.faults = faults;
        this.replayApprovals = new FreshReplayApprovalFactory(toolGateway, approvals);
    }

    @Override
    public EvaluationRecord execute(BenchmarkCase benchmarkCase, BenchmarkRunRequest request, EvaluationRunMetadata metadata) {
        EvaluationRecord record = base(benchmarkCase, metadata);
        if (!"order.refund_execute".equals(benchmarkCase.operationType)) {
            record.executionStatus = CaseExecutionStatus.NOT_EXECUTED;
            record.failureReasons.add(FailureReasonCode.BENCHMARK_TYPE_NOT_IMPLEMENTED.name());
            return record;
        }
        externalSystem.reset(mode(benchmarkCase.externalSystemMode));
        faults.reset();
        armFault(benchmarkCase);

        long taskId = TASK_IDS.incrementAndGet();
        long tenantId = longValue(benchmarkCase.identity.get("tenantId"), 1L);
        long shopId = longValue(benchmarkCase.identity.get("shopId"), 1L);
        long userId = longValue(benchmarkCase.identity.get("userId"), 1L);
        Map<String, Object> input = new LinkedHashMap<>(benchmarkCase.input);
        String operationRequestId = String.valueOf(input.get("operationRequestId"));
        String orderId = String.valueOf(input.get("orderId"));
        List<Map<String, Object>> deliveries = java.util.Collections.synchronizedList(new ArrayList<>());
        List<IdempotencyAttemptAttribution> attributionAttempts =
                java.util.Collections.synchronizedList(new ArrayList<>());
        List<Map<String, Object>> decisions = new ArrayList<>();
        List<Map<String, Object>> writeSnapshots = new ArrayList<>();
        List<ToolInvokeResult> results = new ArrayList<>();

        try {
            int attempts = intValue(benchmarkCase.deliveryPattern.get("attempts"), 1);
            int workers = intValue(benchmarkCase.concurrency.get("workers"), 1);
            boolean concurrent = Boolean.TRUE.equals(benchmarkCase.concurrency.get("simultaneous")) || workers > 1;
            if (concurrent) {
                List<FreshReplayApprovalFactory.ReplayApproval> attemptApprovals = new ArrayList<>();
                for (int attempt = 1; attempt <= attempts; attempt++) {
                    attemptApprovals.add(freshApproval(tenantId, shopId, userId, taskId, attempt, input));
                }
                results.addAll(runConcurrent(attempts, workers, attempt -> () -> executeDelivery(
                        attempt, attempt == 1 ? "INITIAL" : "REPLAY", attempt > 1,
                        tenantId, shopId, userId, taskId, attemptApprovals.get(attempt - 1), input,
                        deliveries, attributionAttempts)));
            } else {
                for (int attempt = 1; attempt <= attempts; attempt++) {
                    FreshReplayApprovalFactory.ReplayApproval approval =
                            freshApproval(tenantId, shopId, userId, taskId, attempt, input);
                    results.add(executeDelivery(
                            attempt, attempt == 1 ? "INITIAL" : "REPLAY", attempt > 1,
                            tenantId, shopId, userId, taskId, approval, input,
                            deliveries, attributionAttempts));
                }
            }

            if (Boolean.TRUE.equals(benchmarkCase.idempotencyExpectation.get("exercisePayloadConflict"))) {
                Map<String, Object> changed = new LinkedHashMap<>(input);
                changed.put("refundAmount", intValue(input.get("refundAmount"), 0) + 1);
                int conflictAttempt = deliveries.size() + 1;
                FreshReplayApprovalFactory.ReplayApproval conflictApproval =
                        freshApproval(tenantId, shopId, userId, taskId, conflictAttempt, changed);
                ToolInvokeResult conflict = executeDelivery(
                        conflictAttempt, "PAYLOAD_CONFLICT", true,
                        tenantId, shopId, userId, taskId, conflictApproval, changed,
                        deliveries, attributionAttempts);
                results.add(conflict);
            }
        } catch (RuntimeException ex) {
            record.observedFacts.put("driverError", safe(ex.getMessage()));
        }

        for (ToolInvokeResult result : results) {
            if ("IDEMPOTENCY_PAYLOAD_MISMATCH".equals(result.getErrorCode())) {
                decisions.add(Map.of("decision", "PAYLOAD_MISMATCH", "errorCode", result.getErrorCode()));
            } else if (Boolean.TRUE.equals(result.getSuccess()) && result.getData() instanceof Map<?, ?> data
                    && Boolean.TRUE.equals(data.get("idempotentReplay"))) {
                decisions.add(Map.of("decision", "IDEMPOTENT_REPLAY"));
            } else if ("OPERATION_IN_PROGRESS".equals(result.getErrorCode())) {
                decisions.add(Map.of("decision", "IN_PROGRESS_BLOCK"));
            } else if ("EXTERNAL_RESULT_UNKNOWN".equals(result.getErrorCode())) {
                decisions.add(Map.of("decision", "EXTERNAL_UNKNOWN_BLOCK"));
            }
        }

        List<WriteOperation> operations = writeOperations.listByTaskId(tenantId, shopId, taskId);
        for (WriteOperation operation : operations) writeSnapshots.add(writeMap(operation));
        CollectedEvidence evidence = evidence(benchmarkCase, taskId, tenantId, shopId, operationRequestId, orderId,
                deliveries, attributionAttempts, decisions, writeSnapshots);
        EvaluationResult evaluation = evaluator.evaluate(benchmarkCase, evidence);
        populate(record, benchmarkCase, evidence, evaluation, taskId);
        return record;
    }

    private ToolInvokeResult invoke(long tenantId, long shopId, long userId, long taskId, Long approvalId, Map<String, Object> input) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(tenantId);
        context.setShopId(shopId);
        context.setUserId(userId);
        context.setTaskId(taskId);
        context.setTraceId("idem-" + taskId);
        context.setApprovalId(approvalId);
        context.setPermissions(Set.of("order:read", "order:refund"));
        context.setManualInvoke(true);
        return toolGateway.invoke(context, "order.refund_execute", input);
    }

    private FreshReplayApprovalFactory.ReplayApproval freshApproval(
            long tenantId, long shopId, long userId, long taskId, int attempt, Map<String, Object> input) {
        return replayApprovals.createApproved(
                tenantId, shopId, userId, taskId, "idem-" + taskId + "-approval-" + attempt, input);
    }

    private ToolInvokeResult executeDelivery(
            int attempt,
            String attemptKind,
            boolean intendedReplay,
            long tenantId,
            long shopId,
            long userId,
            long taskId,
            FreshReplayApprovalFactory.ReplayApproval approval,
            Map<String, Object> input,
            List<Map<String, Object>> deliveries,
            List<IdempotencyAttemptAttribution> attributionAttempts) {
        Instant started = Instant.now();
        ToolInvokeResult result;
        try {
            result = invoke(tenantId, shopId, userId, taskId, approval.approvalId(), input);
        } catch (RuntimeException ex) {
            result = ToolInvokeResult.failed("DRIVER_DELIVERY_EXCEPTION", safe(ex.getMessage()), null);
        }
        String approvalStatus = approvals.get(tenantId, shopId, approval.approvalId())
                .map(ApprovalRequestDto::getStatus).orElse(null);
        String logicalOperationId = String.valueOf(input.get("operationRequestId"));
        boolean writeOperationExistsAfter = writeOperations.listByTaskId(tenantId, shopId, taskId).stream()
                .anyMatch(operation -> logicalOperationId.equals(operation.getOperationRequestId()));
        boolean externalAttemptObserved = externalAttemptImpliedByResult(result, writeOperationExistsAfter);
        IdempotencyAttemptAttribution attribution = IdempotencyAttributionClassifier.classify(
                attempt, attemptKind, intendedReplay, approval.approvalId(), approvalStatus,
                result, writeOperationExistsAfter, externalAttemptObserved);
        attributionAttempts.add(attribution);
        Map<String, Object> delivery = new LinkedHashMap<>();
        delivery.put("deliveryAttempt", attempt);
        delivery.put("attemptKind", attemptKind);
        delivery.put("approvalId", approval.approvalId());
        delivery.put("status", String.valueOf(result.getStatus()));
        delivery.put("errorCode", result.getErrorCode() == null ? "" : result.getErrorCode());
        delivery.put("startedAt", started.toString());
        delivery.put("approvalPassed", attribution.approvalPassed());
        delivery.put("writeOperationBoundaryReached", attribution.writeOperationBoundaryReached());
        delivery.put("preIdempotencyBlocked", attribution.preIdempotencyBlocked());
        delivery.put("attributionCode", attribution.attributionCode());
        deliveries.add(delivery);
        return result;
    }

    private boolean externalAttemptImpliedByResult(ToolInvokeResult result, boolean writeOperationExistsAfter) {
        if (result == null || !writeOperationExistsAfter) return false;
        if (Boolean.TRUE.equals(result.getSuccess())) {
            if (result.getData() instanceof Map<?, ?> data && Boolean.TRUE.equals(data.get("idempotentReplay"))) {
                return false;
            }
            return true;
        }
        String error = result.getErrorCode() == null ? "" : result.getErrorCode();
        return "EXTERNAL_RESULT_UNKNOWN".equals(error)
                || "EXTERNAL_REJECTED".equals(error)
                || "TOOL_EXECUTE_ERROR".equals(error);
    }

    private List<ToolInvokeResult> runConcurrent(
            int attempts,
            int workers,
            IntFunction<Callable<ToolInvokeResult>> callFactory) {
        int safeAttempts = Math.max(1, attempts);
        int safeWorkers = Math.max(2, Math.min(workers, safeAttempts));
        ExecutorService pool = Executors.newFixedThreadPool(safeWorkers);
        CountDownLatch ready = new CountDownLatch(safeAttempts);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ToolInvokeResult>> futures = new ArrayList<>();
            for (int i = 1; i <= safeAttempts; i++) {
                int attempt = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return callFactory.apply(attempt).call();
                }));
            }
            ready.await();
            start.countDown();
            List<ToolInvokeResult> results = new ArrayList<>();
            for (Future<ToolInvokeResult> future : futures) results.add(future.get());
            return results;
        } catch (Exception ex) {
            throw new IllegalStateException("Concurrent delivery failed", ex);
        } finally {
            pool.shutdownNow();
        }
    }

    private CollectedEvidence evidence(BenchmarkCase c, long taskId, long tenantId, long shopId,
                                        String logicalOperationId, String orderId,
                                        List<Map<String, Object>> deliveries,
                                        List<IdempotencyAttemptAttribution> attributionAttempts,
                                        List<Map<String, Object>> decisions,
                                        List<Map<String, Object>> writeSnapshots) {
        CollectedEvidence evidence = new CollectedEvidence();
        evidence.businessFacts.put("externalGroundTruthAvailable", true);
        evidence.businessFacts.put("externalSystemMode", externalSystemMode(c));
        evidence.logicalWriteRequests.add(Map.of(
                "logicalOperationId", logicalOperationId,
                "operationType", c.operationType,
                "businessTarget", orderId));
        evidence.deliveryAttempts.addAll(deliveries);
        for (IdempotencyAttemptAttribution attempt : attributionAttempts) {
            evidence.idempotencyAttributionAttempts.add(attempt.toMap());
            if (attempt.writeOperationBoundaryReached()) {
                evidence.executionAttempts.add(attempt.toMap());
            }
        }
        IdempotencyAttributionEligibility.Result attribution =
                new IdempotencyAttributionEligibility().evaluate(attributionAttempts);
        evidence.businessFacts.put("attributionEvidenceRequired", true);
        evidence.businessFacts.put("attributionEligible", attribution.eligible());
        evidence.businessFacts.put("intendedIdempotencyAttempts", attribution.intendedAttempts());
        evidence.businessFacts.put("intendedReplayAttempts",
                attributionAttempts.stream().filter(IdempotencyAttemptAttribution::intendedReplay).count());
        evidence.businessFacts.put("approvalSetupActivities", attributionAttempts.size());
        evidence.businessFacts.put("idempotencyBoundaryReachedAttempts", attribution.boundaryReachedAttempts());
        evidence.businessFacts.put("preIdempotencyBlockedAttempts", attribution.preIdempotencyBlockedAttempts());
        evidence.businessFacts.put("attributionInvalidReasons", attribution.reasons());
        evidence.toolLogs.addAll(toolLogs.listByTaskId(tenantId, shopId, taskId));
        ApprovalRequestQueryParam approvalQuery = new ApprovalRequestQueryParam();
        approvalQuery.setTaskId(taskId);
        approvalQuery.setPageSize(100);
        evidence.approvals.addAll(approvals.list(tenantId, shopId, approvalQuery).getList());
        for (ExternalAttempt attempt : externalSystem.attempts()) evidence.externalAttempts.add(attemptMap(attempt));
        for (ExternalSideEffect effect : externalSystem.effects()) evidence.externalEffects.add(effectMap(effect));
        evidence.sideEffects.addAll(evidence.externalEffects);
        evidence.idempotencyDecisions.addAll(decisions);
        evidence.writeOperationTransitions.addAll(writeSnapshots);
        evidence.faultEvents.addAll(faults.events());
        evidence.evidenceRefs.addAll(evidenceRefs(externalSystem.effects(), writeSnapshots));
        return evidence;
    }

    private void populate(EvaluationRecord record, BenchmarkCase c, CollectedEvidence evidence,
                          EvaluationResult evaluation, long taskId) {
        record.taskId = taskId;
        record.runtimeMetadata.put("externalSystemMode", externalSystemMode(c));
        record.runtimeMetadata.put("faultScenario", String.valueOf(c.faultInjection.getOrDefault("scenario", "NONE")));
        record.runtimeMetadata.put("concurrencyMode", intValue(c.concurrency.get("workers"), 1) > 1 ? "CONCURRENT" : "SEQUENTIAL");
        record.observedFacts.putAll(evidence.businessFacts);
        for (Map<String, Object> toolLog : evidence.toolLogs) record.toolAttempts.add(compactToolLog(toolLog));
        for (ApprovalRequestDto approval : evidence.approvals) record.approvalEvents.add(approvalMap(approval));
        record.writeOperations.addAll(evidence.writeOperationTransitions);
        record.sideEffects.addAll(evidence.externalEffects);
        record.faultEvents.addAll(evidence.faultEvents);
        record.evidenceRefs.addAll(evidence.evidenceRefs);
        record.businessOutcome.put("externalEffects", evidence.externalEffects.size());
        record.businessOutcome.put("externalAttempts", evidence.externalAttempts.size());
        record.businessOutcome.put("attributionEligible", evidence.businessFacts.get("attributionEligible"));
        record.businessOutcome.put("intendedReplayAttempts",
                evidence.businessFacts.get("intendedReplayAttempts"));
        record.businessOutcome.put("idempotencyBoundaryReachedAttempts",
                evidence.businessFacts.get("idempotencyBoundaryReachedAttempts"));
        record.businessOutcome.put("preIdempotencyBlockedAttempts",
                evidence.businessFacts.get("preIdempotencyBlockedAttempts"));
        record.metricBreakdown.logicalWriteRequests = number(evaluation.metricValues.get("logicalWriteRequests"));
        record.metricBreakdown.deliveryAttempts = number(evaluation.metricValues.get("deliveryAttempts"));
        record.metricBreakdown.executionAttempts = number(evaluation.metricValues.get("executionAttempts"));
        record.metricBreakdown.externalAttempts = number(evaluation.metricValues.get("externalAttempts"));
        record.metricBreakdown.expectedLogicalSideEffects = number(evaluation.metricValues.get("expectedLogicalEffects"));
        record.metricBreakdown.actualEffectiveSideEffects = number(evaluation.metricValues.get("actualEffectiveEffects"));
        record.metricBreakdown.duplicateSideEffects = number(evaluation.metricValues.get("duplicateEffects"));
        record.metricBreakdown.missingSideEffects = number(evaluation.metricValues.get("missingEffects"));
        record.metricBreakdown.intendedReplayAttempts =
                number(evaluation.metricValues.get("intendedReplayAttempts"));
        record.metricBreakdown.idempotencyBoundaryReachedAttempts =
                number(evaluation.metricValues.get("idempotencyBoundaryReachedAttempts"));
        record.metricBreakdown.preIdempotencyBlockedAttempts =
                number(evaluation.metricValues.get("preIdempotencyBlockedAttempts"));
        record.metricBreakdown.attributionEligible =
                boolMetric(evaluation.metricValues.get("attributionEligible"));
        record.failureReasons.addAll(evaluation.failureReasons.stream().map(Enum::name).toList());
        record.executionStatus = evaluation.passed ? CaseExecutionStatus.PASSED : CaseExecutionStatus.FAILED;
        record.finalState = evidence.writeOperationTransitions.isEmpty() ? null
                : String.valueOf(evidence.writeOperationTransitions.get(evidence.writeOperationTransitions.size() - 1).get("status"));
    }

    private EvaluationRecord base(BenchmarkCase c, EvaluationRunMetadata metadata) {
        EvaluationRecord record = new EvaluationRecord();
        record.caseId = c.caseId;
        record.scenario = c.scenario;
        record.difficulty = c.difficulty;
        record.tags.addAll(c.tags == null ? List.of() : c.tags);
        record.origin = c.origin;
        record.input.putAll(c.input);
        record.evaluationRunId = metadata.runId;
        record.runtimeMetadata.put("environment", metadata.environment);
        record.runtimeMetadata.put("executionLevel", metadata.executionLevel);
        record.runtimeMetadata.put("databaseMode", metadata.databaseMode);
        record.runtimeMetadata.put("queueMode", metadata.queueMode);
        return record;
    }

    private void armFault(BenchmarkCase c) {
        Object point = c.faultInjection.get("point");
        if (point == null || String.valueOf(point).isBlank() || "NONE".equalsIgnoreCase(String.valueOf(point))) return;
        faults.arm(ReliabilityFaultPoint.valueOf(String.valueOf(point)), intValue(c.faultInjection.get("triggerAt"), 1));
    }

    private ExternalSystemMode mode(String value) {
        if (value == null || value.isBlank()) return ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL;
        return ExternalSystemMode.valueOf(value);
    }

    private String externalSystemMode(BenchmarkCase c) {
        return c.externalSystemMode == null ? ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL.name() : c.externalSystemMode;
    }

    private Map<String, Object> attemptMap(ExternalAttempt attempt) {
        return Map.of(
                "attemptNo", attempt.attemptNo(),
                "sourceType", "EXTERNAL_TEST_SYSTEM",
                "logicalOperationId", attempt.logicalOperationId(),
                "businessTarget", attempt.businessTarget(),
                "payloadHash", attempt.payloadHash(),
                "simulation", attempt.simulation(),
                "outcome", attempt.outcome(),
                "externalEffectId", attempt.externalEffectId() == null ? "" : attempt.externalEffectId(),
                "attemptedAt", attempt.attemptedAt().toString());
    }

    private Map<String, Object> effectMap(ExternalSideEffect effect) {
        return Map.of(
                "sourceType", "EXTERNAL_TEST_SYSTEM",
                "externalEffectId", effect.externalEffectId(),
                "operationType", effect.operationType(),
                "logicalOperationId", effect.logicalOperationId(),
                "businessTarget", effect.businessTarget(),
                "payloadHash", effect.payloadHash(),
                "acceptedAt", effect.acceptedAt().toString(),
                "externalStatus", effect.externalStatus());
    }

    private Map<String, Object> writeMap(WriteOperation operation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("writeOperationId", operation.getId());
        map.put("logicalOperationId", operation.getOperationRequestId());
        map.put("idempotencyKey", operation.getIdempotencyKey());
        map.put("inputHash", operation.getInputHash());
        map.put("businessTarget", operation.getBusinessObjectId());
        map.put("status", operation.getStatus());
        map.put("externalReference", operation.getExternalReference());
        map.put("version", operation.getVersion());
        map.put("freshExecution", operation.isFreshExecution());
        return map;
    }

    private Map<String, Object> compactToolLog(Map<String, Object> log) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("id", log.get("id"));
        compact.put("toolCode", log.get("toolCode"));
        compact.put("status", log.get("status"));
        compact.put("riskLevel", log.get("riskLevel"));
        compact.put("approvalId", log.get("approvalId"));
        compact.put("errorCode", log.get("errorCode"));
        compact.put("latencyMs", log.get("latencyMs"));
        compact.put("createdAt", log.get("createdAt"));
        if (log.containsKey("input")) compact.put("inputHash", sha256(String.valueOf(log.get("input"))));
        if (log.containsKey("inputJson")) compact.put("inputHash", sha256(String.valueOf(log.get("inputJson"))));
        if (log.containsKey("output")) compact.put("outputHash", sha256(String.valueOf(log.get("output"))));
        if (log.containsKey("outputJson")) compact.put("outputHash", sha256(String.valueOf(log.get("outputJson"))));
        return compact;
    }

    private Map<String, Object> approvalMap(ApprovalRequestDto approval) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("approvalId", approval.getApprovalId());
        map.put("toolCode", approval.getToolCode());
        map.put("riskLevel", approval.getRiskLevel());
        map.put("status", approval.getStatus());
        map.put("requesterId", approval.getRequesterId());
        map.put("approverId", approval.getApproverId());
        map.put("inputHash", approval.getInputHash());
        map.put("businessObjectId", approval.getBusinessObjectId());
        return map;
    }

    private List<EvidenceRef> evidenceRefs(List<ExternalSideEffect> effects, List<Map<String, Object>> writes) {
        List<EvidenceRef> refs = new ArrayList<>();
        for (ExternalSideEffect effect : effects) {
            refs.add(new EvidenceRef("EXTERNAL_TEST_SYSTEM", effect.externalEffectId(),
                    "effective refund effect for logicalOperationId=" + effect.logicalOperationId(),
                    sha256(effect.toString()), effect.acceptedAt()));
        }
        for (Map<String, Object> write : writes) {
            refs.add(new EvidenceRef("PRODUCTION_WRITE_OPERATION", String.valueOf(write.get("writeOperationId")),
                    "ShopOps write state " + write.get("status"), sha256(write.toString()), Instant.now()));
        }
        return refs;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        return Integer.parseInt(String.valueOf(value));
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return fallback;
        return Long.parseLong(String.valueOf(value));
    }

    private Boolean boolMetric(Object value) { return value instanceof Boolean b ? b : null; }
    private Integer number(Object value) { return value instanceof Number n ? n.intValue() : null; }
    private String safe(String value) { return value == null ? "unavailable" : value; }
}
