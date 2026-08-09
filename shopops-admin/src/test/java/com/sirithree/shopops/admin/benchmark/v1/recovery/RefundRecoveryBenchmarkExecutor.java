package com.sirithree.shopops.admin.benchmark.v1.recovery;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evidence.EvidenceRef;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSideEffect;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import com.sirithree.shopops.admin.reliability.service.WriteOperationReconciliationService;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 4 recovery driver. It creates the failure through the real ToolGateway write path and drives recovery only
 * through WriteOperationReconciliationService. It never mutates a production status itself.
 */
public class RefundRecoveryBenchmarkExecutor implements RecoveryBenchmarkExecutor {
    private static final AtomicLong TASK_IDS = new AtomicLong(940000);
    private final ToolGatewayService toolGateway;
    private final ApprovalRequestService approvals;
    private final WriteOperationService writeOperations;
    private final WriteOperationReconciliationService reconciliation;
    private final RecordingRefundExternalSystem external;
    private final DeterministicReliabilityFaultController faults;
    private final StateConvergenceEvaluator evaluator = new StateConvergenceEvaluator();

    public RefundRecoveryBenchmarkExecutor(ToolGatewayService toolGateway,
                                           ApprovalRequestService approvals,
                                           WriteOperationService writeOperations,
                                           WriteOperationReconciliationService reconciliation,
                                           RecordingRefundExternalSystem external,
                                           DeterministicReliabilityFaultController faults) {
        this.toolGateway = toolGateway;
        this.approvals = approvals;
        this.writeOperations = writeOperations;
        this.reconciliation = reconciliation;
        this.external = external;
        this.faults = faults;
    }

    @Override
    public EvaluationRecord execute(BenchmarkCase c, BenchmarkRunRequest request, EvaluationRunMetadata metadata) {
        EvaluationRecord record = base(c, metadata);
        if (!"order.refund_execute".equals(c.operationType)) {
            record.executionStatus = CaseExecutionStatus.NOT_EXECUTED;
            record.failureReasons.add(FailureReasonCode.BENCHMARK_TYPE_NOT_IMPLEMENTED.name());
            return record;
        }
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        faults.reset();
        reconciliation.setMaxRecoveryAttempts(c.maxRecoveryAttempts == null ? 3 : c.maxRecoveryAttempts);
        armInitialFault(c);

        long taskId = TASK_IDS.incrementAndGet();
        long tenantId = longValue(c.identity.get("tenantId"), 1L);
        long shopId = longValue(c.identity.get("shopId"), 1L);
        long userId = longValue(c.identity.get("userId"), 1L);
        Map<String, Object> input = new LinkedHashMap<>(c.input);
        String operationRequestId = String.valueOf(input.get("operationRequestId"));
        List<Map<String, Object>> recoveryEvents = new ArrayList<>();

        try {
            Long approvalId = createAndApprove(tenantId, shopId, userId, taskId, input);
            invoke(tenantId, shopId, userId, taskId, approvalId, input);
        } catch (RuntimeException ex) {
            record.observedFacts.put("initialExecutionException", safe(ex.getMessage()));
        }

        List<WriteOperation> initialWrites = writeOperations.listByTaskId(tenantId, shopId, taskId);
        WriteOperation operation = initialWrites.isEmpty() ? null : initialWrites.get(initialWrites.size() - 1);
        if (operation == null) {
            record.executionStatus = CaseExecutionStatus.ERROR;
            record.failureReasons.add(FailureReasonCode.EVALUATION_ERROR.name());
            record.observedFacts.put("driverError", "write operation not created");
            return record;
        }

        armRecoveryFaults(c);
        int maxAttempts = c.maxRecoveryAttempts == null ? 3 : c.maxRecoveryAttempts;
        if ("DUPLICATE_RECONCILIATION".equals(c.scenario)) {
            runConcurrentRecovery(operation, recoveryEvents);
        } else {
            for (int i = 1; i <= maxAttempts; i++) {
                WriteOperation current = writeOperations.findByKey(operation.getIdempotencyKey());
                if (current == null || WriteOperationStatus.terminal(current.getStatus())) break;
                WriteOperationReconciliationService.RecoveryResult rr = reconciliation.reconcileOperation(current);
                recoveryEvents.add(recoveryEvent(i, rr));
                if (rr.terminalStateReached()) break;
            }
        }

        WriteOperation finalOperation = writeOperations.findByKey(operation.getIdempotencyKey());
        CollectedEvidence evidence = new CollectedEvidence();
        String externalReality = external.reality(operationRequestId);
        evidence.businessFacts.put("externalReality", externalReality);
        evidence.businessFacts.put("localState", finalOperation == null ? "UNAVAILABLE" : finalOperation.getStatus());
        evidence.businessFacts.put("recoveryAttempts", finalOperation == null || finalOperation.getRecoveryAttemptCount() == null ? 0 : finalOperation.getRecoveryAttemptCount());
        evidence.businessFacts.put("recoveryReason", finalOperation == null ? "UNAVAILABLE" : finalOperation.getLastErrorCode());
        evidence.businessFacts.put("effectiveSideEffects", external.effectiveEffectCount());
        evidence.businessFacts.put("logicalOperationId", operationRequestId);
        evidence.writeOperationTransitions.addAll(writeMaps(initialWrites));
        if (finalOperation != null) evidence.writeOperationTransitions.add(writeMap(finalOperation));
        evidence.externalEffects.addAll(external.effects().stream().map(this::effectMap).toList());
        evidence.sideEffects.addAll(evidence.externalEffects);
        evidence.faultEvents.addAll(faults.events());
        evidence.evidenceRefs.addAll(evidenceRefs(external.effects(), finalOperation));

        EvaluationResult evaluation = evaluator.evaluate(c, evidence);
        populate(record, evidence, evaluation, finalOperation, recoveryEvents);
        return record;
    }

    private Long createAndApprove(long tenantId, long shopId, long userId, long taskId, Map<String, Object> input) {
        ToolInvokeResult pending = invoke(tenantId, shopId, userId, taskId, null, input);
        if (!"APPROVAL_REQUIRED".equals(pending.getStatus()) || pending.getApprovalId() == null) {
            throw new IllegalStateException("Expected approval before refund execution but got " + pending.getStatus());
        }
        ApprovalDecisionParam decision = new ApprovalDecisionParam();
        decision.setComment("ShopOpsBench Phase 4 deterministic approval");
        decision.setConfirmText("确认通过");
        ApprovalRequestDto approved = approvals.approve(tenantId, shopId, pending.getApprovalId(), userId,
                        "benchmark-approver", decision)
                .orElseThrow(() -> new IllegalStateException("Approval transition failed: " + pending.getApprovalId()));
        return approved.getApprovalId();
    }

    private ToolInvokeResult invoke(long tenantId, long shopId, long userId, long taskId, Long approvalId, Map<String, Object> input) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(tenantId);
        context.setShopId(shopId);
        context.setUserId(userId);
        context.setTaskId(taskId);
        context.setTraceId("recovery-" + taskId);
        context.setApprovalId(approvalId);
        context.setPermissions(Set.of("order:read", "order:refund"));
        context.setManualInvoke(true);
        return toolGateway.invoke(context, "order.refund_execute", input);
    }

    private void armInitialFault(BenchmarkCase c) {
        if (c.faultPoint == null || c.faultPoint.isBlank()) return;
        ReliabilityFaultPoint point = ReliabilityFaultPoint.valueOf(c.faultPoint);
        if (point == ReliabilityFaultPoint.AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM
                || point == ReliabilityFaultPoint.AFTER_LOCAL_CONFIRM_BEFORE_ACK) {
            faults.arm(point, intValue(c.faultInjection.get("triggerAt"), 1));
        }
    }

    private void armRecoveryFaults(BenchmarkCase c) {
        if ("RECONCILIATION_TEMPORARY_FAILURE".equals(c.scenario)) {
            faults.arm(ReliabilityFaultPoint.BEFORE_RECONCILIATION_QUERY, 1);
        }
        if ("RECOVERY_BUDGET_EXHAUSTED".equals(c.scenario)) {
            int max = c.maxRecoveryAttempts == null ? 3 : c.maxRecoveryAttempts;
            for (int i = 1; i <= max; i++) faults.arm(ReliabilityFaultPoint.BEFORE_RECONCILIATION_QUERY, i);
        }
        if ("RECOVERY_STATE_UPDATE_FAILURE".equals(c.scenario)) {
            faults.arm(ReliabilityFaultPoint.BEFORE_RECOVERY_STATE_UPDATE, 1);
        }
    }

    private void runConcurrentRecovery(WriteOperation operation, List<Map<String, Object>> events) {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<WriteOperationReconciliationService.RecoveryResult>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    WriteOperation current = writeOperations.findByKey(operation.getIdempotencyKey());
                    return reconciliation.reconcileOperation(current);
                }));
            }
            ready.await();
            start.countDown();
            int i = 0;
            for (Future<WriteOperationReconciliationService.RecoveryResult> future : futures) {
                events.add(recoveryEvent(++i, future.get()));
            }
        } catch (Exception ex) {
            events.add(Map.of("error", safe(ex.getMessage())));
        } finally {
            pool.shutdownNow();
        }
    }

    private Map<String, Object> recoveryEvent(int attempt, WriteOperationReconciliationService.RecoveryResult rr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("attempt", attempt);
        m.put("terminal", rr.terminalStateReached());
        m.put("stateCorrect", rr.stateCorrect());
        m.put("reason", rr.reason());
        if (rr.operation() != null) m.put("localState", rr.operation().getStatus());
        return m;
    }

    private void populate(EvaluationRecord record, CollectedEvidence evidence, EvaluationResult evaluation,
                          WriteOperation finalOperation, List<Map<String, Object>> recoveryEvents) {
        record.observedFacts.putAll(evidence.businessFacts);
        record.writeOperations.addAll(evidence.writeOperationTransitions);
        record.sideEffects.addAll(evidence.externalEffects);
        record.faultEvents.addAll(evidence.faultEvents);
        record.stateTransitions.addAll(recoveryEvents);
        record.evidenceRefs.addAll(evidence.evidenceRefs);
        record.finalState = finalOperation == null ? null : finalOperation.getStatus();
        record.metricBreakdown.terminalStateReached = bool(evaluation, "terminalStateReached");
        record.metricBreakdown.localStateConsistentWithExternalReality = bool(evaluation, "localStateConsistentWithExternalReality");
        record.metricBreakdown.converged = bool(evaluation, "converged");
        record.metricBreakdown.recoveryAttempts = number(evaluation.metricValues.get("recoveryAttempts"));
        record.metricBreakdown.reconciliationAttempts = record.metricBreakdown.recoveryAttempts;
        record.metricBreakdown.manualReviewCount = number(evaluation.metricValues.get("manualReviewCount"));
        record.metricBreakdown.permanentStuckCount = number(evaluation.metricValues.get("permanentStuckCount"));
        record.metricBreakdown.incorrectTerminalStateCount = number(evaluation.metricValues.get("incorrectTerminalStateCount"));
        record.metricBreakdown.actualEffectiveSideEffects = external.effectiveEffectCount();
        record.metricBreakdown.duplicateSideEffects = number(evaluation.metricValues.get("duplicateSideEffects"));
        record.failureReasons.addAll(evaluation.failureReasons.stream().map(Enum::name).toList());
        record.executionStatus = evaluation.passed ? CaseExecutionStatus.PASSED : CaseExecutionStatus.FAILED;
    }

    private EvaluationRecord base(BenchmarkCase c, EvaluationRunMetadata metadata) {
        EvaluationRecord r = new EvaluationRecord();
        r.caseId = c.caseId;
        r.scenario = c.scenario;
        r.difficulty = c.difficulty;
        r.tags.addAll(c.tags == null ? List.of() : c.tags);
        r.origin = c.origin;
        r.input.putAll(c.input);
        r.evaluationRunId = metadata.runId;
        r.runtimeMetadata.put("environment", metadata.environment);
        r.runtimeMetadata.put("executionLevel", metadata.executionLevel);
        r.runtimeMetadata.put("databaseMode", metadata.databaseMode);
        r.runtimeMetadata.put("externalSystemMode", metadata.externalSystemMode);
        return r;
    }

    private List<Map<String, Object>> writeMaps(List<WriteOperation> operations) {
        return operations.stream().map(this::writeMap).toList();
    }

    private Map<String, Object> writeMap(WriteOperation operation) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("writeOperationId", operation.getId());
        m.put("logicalOperationId", operation.getOperationRequestId());
        m.put("status", operation.getStatus());
        m.put("externalReference", operation.getExternalReference());
        m.put("recoveryAttemptCount", operation.getRecoveryAttemptCount());
        m.put("lastRecoveryAt", operation.getLastRecoveryAt());
        m.put("lastErrorCode", operation.getLastErrorCode());
        m.put("retryAction", operation.getRetryAction());
        m.put("version", operation.getVersion());
        return m;
    }

    private Map<String, Object> effectMap(ExternalSideEffect e) {
        return Map.of("sourceType", "EXTERNAL_TEST_SYSTEM", "externalEffectId", e.externalEffectId(),
                "logicalOperationId", e.logicalOperationId(), "businessTarget", e.businessTarget(),
                "externalStatus", e.externalStatus(), "acceptedAt", e.acceptedAt().toString());
    }

    private List<EvidenceRef> evidenceRefs(List<ExternalSideEffect> effects, WriteOperation operation) {
        List<EvidenceRef> refs = new ArrayList<>();
        for (ExternalSideEffect effect : effects) {
            refs.add(new EvidenceRef("EXTERNAL_TEST_SYSTEM", effect.externalEffectId(),
                    "refund external reality", sha256(effect.toString()), effect.acceptedAt()));
        }
        if (operation != null) {
            refs.add(new EvidenceRef("PRODUCTION_WRITE_OPERATION", String.valueOf(operation.getId()),
                    "final local write state=" + operation.getStatus(), sha256(writeMap(operation).toString()), Instant.now()));
        }
        return refs;
    }

    private Boolean bool(EvaluationResult e, String key) { Object v = e.metricValues.get(key); return v instanceof Boolean b ? b : null; }
    private Integer number(Object v) { return v instanceof Number n ? n.intValue() : null; }
    private int intValue(Object v, int fallback) { return v instanceof Number n ? n.intValue() : v == null ? fallback : Integer.parseInt(String.valueOf(v)); }
    private long longValue(Object v, long fallback) { return v instanceof Number n ? n.longValue() : v == null ? fallback : Long.parseLong(String.valueOf(v)); }
    private String safe(String v) { return v == null ? "unavailable" : v; }
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
