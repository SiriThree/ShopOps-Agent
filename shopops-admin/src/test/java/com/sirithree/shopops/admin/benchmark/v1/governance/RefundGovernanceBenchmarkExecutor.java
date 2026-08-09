package com.sirithree.shopops.admin.benchmark.v1.governance;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.auth.domain.DataScope;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evidence.EvidenceRef;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSideEffect;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 5 Tool-Gateway governance driver. Attack payloads are deterministic benchmark input, but all authorization,
 * schema, risk, approval, write and external-side-effect decisions are made by production components.
 */
public class RefundGovernanceBenchmarkExecutor implements GovernanceBenchmarkExecutor {
    private static final AtomicLong TASK_IDS = new AtomicLong(950000);
    private final ToolGatewayService toolGateway;
    private final ApprovalRequestService approvals;
    private final WriteOperationService writeOperations;
    private final RecordingRefundExternalSystem external;
    private final AuthorizationService authorization;
    private final ExecutionGovernanceEvaluator evaluator = new ExecutionGovernanceEvaluator();

    public RefundGovernanceBenchmarkExecutor(ToolGatewayService toolGateway,
                                             ApprovalRequestService approvals,
                                             WriteOperationService writeOperations,
                                             RecordingRefundExternalSystem external,
                                             AuthorizationService authorization) {
        this.toolGateway = toolGateway;
        this.approvals = approvals;
        this.writeOperations = writeOperations;
        this.external = external;
        this.authorization = authorization;
    }

    @Override
    public EvaluationRecord execute(BenchmarkCase c, BenchmarkRunRequest request, EvaluationRunMetadata metadata) {
        EvaluationRecord record = base(c, metadata);
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        if (authorization instanceof GovernanceAuthorizationFixture fixture) {
            fixture.reset();
        }

        long tenantId = longValue(c.identity.get("tenantId"), 1L);
        long shopId = longValue(c.identity.get("shopId"), 1L);
        long userId = longValue(c.identity.get("userId"), 1L);
        long taskId = TASK_IDS.incrementAndGet();
        configureTrustedAuthorization(c, tenantId, shopId, userId);
        int effectsBeforeFinal = external.effectiveEffectCount();
        ToolInvokeResult finalResult;
        Long approvalId = null;
        Map<String, Object> finalArguments = new LinkedHashMap<>(c.arguments == null ? Map.of() : c.arguments);

        try {
            String setup = string(c.initialState.get("approvalSetup"), "NONE");
            if (!"NONE".equalsIgnoreCase(setup) && !"BEFORE_APPROVAL".equalsIgnoreCase(setup)) {
                ToolInvokeResult pending = invoke(c, tenantId, shopId, userId, taskId, null, finalArguments);
                if (!"APPROVAL_REQUIRED".equals(pending.getStatus()) || pending.getApprovalId() == null) {
                    throw new IllegalStateException("approval setup expected APPROVAL_REQUIRED but got " + pending.getStatus()
                            + "/" + pending.getErrorCode());
                }
                approvalId = pending.getApprovalId();
                if ("REJECTED".equalsIgnoreCase(setup)) {
                    reject(tenantId, shopId, userId, approvalId);
                } else {
                    approve(tenantId, shopId, userId, approvalId);
                }

                if (setup.startsWith("APPROVED_MUTATED") || "APPROVED_TARGET_MISMATCH".equalsIgnoreCase(setup)) {
                    Object configured = c.initialState.get("finalArguments");
                    if (configured instanceof Map<?, ?> map) finalArguments = toStringMap(map);
                }
                if ("APPROVAL_REPLAY".equalsIgnoreCase(setup)) {
                    ToolInvokeResult first = invoke(c, tenantId, shopId, userId, taskId, approvalId, finalArguments);
                    if (!Boolean.TRUE.equals(first.getSuccess())) {
                        throw new IllegalStateException("approval replay setup first execution failed: " + first.getErrorCode());
                    }
                    effectsBeforeFinal = external.effectiveEffectCount();
                }
                if ("IDEMPOTENT_REPLAY_NEW_APPROVAL".equalsIgnoreCase(setup)) {
                    ToolInvokeResult first = invoke(c, tenantId, shopId, userId, taskId, approvalId, finalArguments);
                    if (!Boolean.TRUE.equals(first.getSuccess())) {
                        throw new IllegalStateException("idempotent replay setup first execution failed: " + first.getErrorCode());
                    }
                    effectsBeforeFinal = external.effectiveEffectCount();
                    ToolInvokeResult secondPending = invoke(c, tenantId, shopId, userId, taskId, null, finalArguments);
                    if (!"APPROVAL_REQUIRED".equals(secondPending.getStatus()) || secondPending.getApprovalId() == null) {
                        throw new IllegalStateException("second approval setup failed: " + secondPending.getErrorCode());
                    }
                    approvalId = secondPending.getApprovalId();
                    approve(tenantId, shopId, userId, approvalId);
                }
            }
            finalResult = invoke(c, tenantId, shopId, userId, taskId,
                    "BEFORE_APPROVAL".equalsIgnoreCase(setup) ? null : approvalId, finalArguments);
        } catch (RuntimeException ex) {
            finalResult = ToolInvokeResult.failed("BENCHMARK_DRIVER_EXCEPTION", safe(ex.getMessage()), null);
        }

        CollectedEvidence evidence = collect(c, tenantId, shopId, userId, taskId, finalResult, effectsBeforeFinal);
        EvaluationResult evaluation = evaluator.evaluate(c, evidence);
        populate(record, evidence, evaluation, finalResult);
        return record;
    }

    private void configureTrustedAuthorization(BenchmarkCase c, long tenantId, long shopId, long userId) {
        List<Long> shops = longList(c.identity.get("accessibleShopIds"));
        if (shops.isEmpty()) shops = List.of(shopId);
        List<String> roles = stringList(c.identity.get("roles"));
        Set<String> permissions = new LinkedHashSet<>(stringList(c.identity.get("permissions")));
        if (authorization instanceof GovernanceAuthorizationFixture fixture) {
            fixture.register(tenantId, shopId, userId, shops, roles, permissions, DataScope.ASSIGNED_SHOPS);
        }
    }

    private ToolInvokeResult invoke(BenchmarkCase c, long tenantId, long shopId, long userId, long taskId,
                                    Long approvalId, Map<String, Object> input) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(tenantId);
        context.setShopId(shopId);
        context.setUserId(userId);
        context.setTaskId(taskId);
        context.setTraceId("governance-" + taskId);
        context.setApprovalId(approvalId);
        List<String> contextPermissions = stringList(c.identity.get("contextPermissions"));
        if (contextPermissions.isEmpty()) contextPermissions = stringList(c.identity.get("permissions"));
        context.setPermissions(Set.copyOf(contextPermissions));
        context.setManualInvoke(true);
        return toolGateway.invoke(context, c.toolCode, input);
    }

    private void approve(long tenantId, long shopId, long userId, long approvalId) {
        ApprovalDecisionParam p = new ApprovalDecisionParam();
        p.setComment("ShopOpsBench Phase 5 governance control approval");
        p.setConfirmText("确认通过");
        approvals.approve(tenantId, shopId, approvalId, userId, "benchmark-approver", p)
                .orElseThrow(() -> new IllegalStateException("approval could not be approved: " + approvalId));
    }

    private void reject(long tenantId, long shopId, long userId, long approvalId) {
        ApprovalDecisionParam p = new ApprovalDecisionParam();
        p.setComment("ShopOpsBench deterministic rejection");
        approvals.reject(tenantId, shopId, approvalId, userId, "benchmark-approver", p)
                .orElseThrow(() -> new IllegalStateException("approval could not be rejected: " + approvalId));
    }

    private CollectedEvidence collect(BenchmarkCase c, long tenantId, long shopId, long userId, long taskId,
                                      ToolInvokeResult result, int effectsBeforeFinal) {
        CollectedEvidence e = new CollectedEvidence();
        GovernanceDecision decision = decision(result);
        e.governanceDecision.put("actualDecision", decision.name());
        e.governanceDecision.put("toolCode", c.toolCode);
        e.governanceDecision.put("status", result.getStatus());
        e.governanceDecision.put("errorCode", result.getErrorCode());
        e.governanceDecision.put("errorMessage", safe(result.getErrorMessage()));
        e.governanceDecision.put("approvalId", result.getApprovalId());
        e.governanceDecision.put("riskLevel", c.riskLevel);

        AuthorizationService.AuthorizationSnapshot snapshot = authorization.resolve(tenantId, shopId, userId);
        e.authorizationSnapshot.put("tenantId", tenantId);
        e.authorizationSnapshot.put("shopId", shopId);
        e.authorizationSnapshot.put("userId", userId);
        e.authorizationSnapshot.put("roles", snapshot.roles());
        e.authorizationSnapshot.put("permissions", snapshot.permissions().stream().sorted().toList());
        e.authorizationSnapshot.put("accessibleShopIds", snapshot.accessibleShopIds());
        e.authorizationSnapshot.put("source", "AUTHORIZATION_FIXTURE");

        ApprovalRequestQueryParam query = new ApprovalRequestQueryParam();
        query.setTaskId(taskId);
        query.setPageSize(100);
        e.approvals.addAll(approvals.list(tenantId, shopId, query).getList());
        List<WriteOperation> writes = writeOperations.listByTaskId(tenantId, shopId, taskId);
        e.writeOperations.addAll(writes);
        e.externalEffects.addAll(external.effects().stream().map(this::effectMap).toList());
        int effectDelta = Math.max(external.effectiveEffectCount() - effectsBeforeFinal, 0);
        boolean approvalBypassed = Boolean.TRUE.equals(c.approvalExpectation.required)
                && decision == GovernanceDecision.ALLOWED
                && e.approvals.stream().noneMatch(a -> "EXECUTED".equals(a.getStatus()));
        e.businessFacts.put("governanceCaseClass", c.governanceCaseClass);
        e.businessFacts.put("attackType", c.attackType);
        e.businessFacts.put("externalEffectDelta", effectDelta);
        e.businessFacts.put("totalExternalEffects", external.effectiveEffectCount());
        e.businessFacts.put("writeOperationCount", writes.size());
        e.businessFacts.put("approvalBypassed", approvalBypassed);
        e.businessFacts.put("externalSystemMode", "NON_IDEMPOTENT_EXTERNAL");
        for (ExternalSideEffect effect : external.effects()) {
            e.evidenceRefs.add(new EvidenceRef("EXTERNAL_TEST_SYSTEM", effect.externalEffectId(),
                    "independent refund side effect", sha256(effect.toString()), effect.acceptedAt()));
        }
        for (ApprovalRequestDto approval : e.approvals) {
            e.evidenceRefs.add(new EvidenceRef("PRODUCTION_APPROVAL", String.valueOf(approval.getApprovalId()),
                    "approval status=" + approval.getStatus() + ", tool=" + approval.getToolCode(),
                    sha256(String.valueOf(approval.getInputHash()) + "|" + approval.getBusinessObjectId()), Instant.now()));
        }
        return e;
    }

    private void populate(EvaluationRecord r, CollectedEvidence e, EvaluationResult result, ToolInvokeResult toolResult) {
        r.governanceDecision = String.valueOf(e.governanceDecision.get("actualDecision"));
        r.authorizationSnapshot.putAll(e.authorizationSnapshot);
        r.observedFacts.putAll(e.businessFacts);
        r.observedFacts.put("governanceDecision", e.governanceDecision);
        r.approvalEvents.addAll(e.approvals.stream().map(this::approvalMap).toList());
        r.writeOperations.addAll(e.writeOperations.stream().map(this::writeMap).toList());
        r.sideEffects.addAll(e.externalEffects);
        r.evidenceRefs.addAll(e.evidenceRefs);
        r.finalState = toolResult.getStatus();
        r.metricBreakdown.governanceSatisfied = result.passed;
        r.metricBreakdown.unauthorizedCase = bool(result, "unauthorizedCase");
        r.metricBreakdown.legitimateCase = bool(result, "legitimateCase");
        r.metricBreakdown.unauthorizedBlocked = bool(result, "unauthorizedBlocked");
        r.metricBreakdown.falseRejected = bool(result, "falseRejected");
        r.metricBreakdown.unauthorizedWriteCount = number(result.metricValues.get("unauthorizedWriteCount"));
        r.metricBreakdown.approvalBypassCount = number(result.metricValues.get("approvalBypassCount"));
        r.metricBreakdown.crossTenantViolationCount = number(result.metricValues.get("crossTenantViolationCount"));
        r.metricBreakdown.crossShopViolationCount = number(result.metricValues.get("crossShopViolationCount"));
        r.metricBreakdown.actualEffectiveSideEffects = number(result.metricValues.get("externalEffectCount"));
        r.failureReasons.addAll(result.failureReasons.stream().map(Enum::name).toList());
        r.executionStatus = result.passed ? CaseExecutionStatus.PASSED : CaseExecutionStatus.FAILED;
    }

    private EvaluationRecord base(BenchmarkCase c, EvaluationRunMetadata metadata) {
        EvaluationRecord r = new EvaluationRecord();
        r.caseId = c.caseId;
        r.scenario = c.scenario;
        r.difficulty = c.difficulty;
        r.tags.addAll(c.tags == null ? List.of() : c.tags);
        r.origin = c.origin;
        r.input.put("toolCode", c.toolCode);
        r.input.put("argumentsHash", sha256(String.valueOf(c.arguments)));
        r.evaluationRunId = metadata.runId;
        r.runtimeMetadata.put("environment", metadata.environment);
        r.runtimeMetadata.put("executionLevel", metadata.executionLevel);
        r.runtimeMetadata.put("authorizationMode", metadata.authorizationMode);
        r.runtimeMetadata.put("databaseMode", metadata.databaseMode);
        r.runtimeMetadata.put("externalSystemMode", metadata.externalSystemMode);
        return r;
    }

    private GovernanceDecision decision(ToolInvokeResult result) {
        if (result == null) return GovernanceDecision.ERROR;
        if (Boolean.TRUE.equals(result.getSuccess())) return GovernanceDecision.ALLOWED;
        if ("APPROVAL_REQUIRED".equals(result.getStatus()) || "APPROVAL_REQUIRED".equals(result.getErrorCode())) {
            return GovernanceDecision.REQUIRES_APPROVAL;
        }
        if ("BENCHMARK_DRIVER_EXCEPTION".equals(result.getErrorCode())) return GovernanceDecision.ERROR;
        return GovernanceDecision.BLOCKED;
    }

    private Map<String, Object> approvalMap(ApprovalRequestDto a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("approvalId", a.getApprovalId());
        m.put("toolCode", a.getToolCode());
        m.put("status", a.getStatus());
        m.put("riskLevel", a.getRiskLevel());
        m.put("businessObjectId", a.getBusinessObjectId());
        m.put("inputHash", a.getInputHash());
        return m;
    }
    private Map<String, Object> writeMap(WriteOperation w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("operationRequestId", w.getOperationRequestId());
        m.put("toolCode", w.getToolCode());
        m.put("status", w.getStatus());
        m.put("externalReference", w.getExternalReference());
        return m;
    }
    private Map<String, Object> effectMap(ExternalSideEffect e) {
        return Map.of("sourceType", "EXTERNAL_TEST_SYSTEM", "externalEffectId", e.externalEffectId(),
                "logicalOperationId", e.logicalOperationId(), "businessTarget", e.businessTarget(),
                "externalStatus", e.externalStatus(), "acceptedAt", e.acceptedAt().toString());
    }
    private Map<String, Object> toStringMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
    private List<Long> longList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) return List.of();
        List<Long> out = new ArrayList<>();
        for (Object item : iterable) out.add(item instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(item)));
        return out;
    }
    private List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object item : iterable) out.add(String.valueOf(item));
        return out;
    }
    private Boolean bool(EvaluationResult e, String key) { Object v = e.metricValues.get(key); return v instanceof Boolean b ? b : null; }
    private Integer number(Object v) { return v instanceof Number n ? n.intValue() : null; }
    private long longValue(Object v, long fallback) { return v instanceof Number n ? n.longValue() : v == null ? fallback : Long.parseLong(String.valueOf(v)); }
    private String string(Object v, String fallback) { return v == null ? fallback : String.valueOf(v); }
    private String safe(String v) { return v == null ? "" : v; }
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
