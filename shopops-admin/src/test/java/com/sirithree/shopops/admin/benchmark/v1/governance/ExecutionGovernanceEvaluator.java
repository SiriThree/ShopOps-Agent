package com.sirithree.shopops.admin.benchmark.v1.governance;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.util.Locale;
import java.util.Map;

/** Observes production governance decisions. It never authorizes, rewrites input, or executes a tool. */
public class ExecutionGovernanceEvaluator {
    public EvaluationResult evaluate(BenchmarkCase c, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        String caseClass = upper(c.governanceCaseClass);
        String expected = upper(c.expectedDecision);
        String actual = upper(evidence.governanceDecision.get("actualDecision"));
        String errorCode = string(evidence.governanceDecision.get("errorCode"));
        String expectedReason = upper(c.expectedReason);
        String actualReason = "ALLOWED".equals(actual) ? "SUCCESS"
                : "REQUIRES_APPROVAL".equals(actual) ? "APPROVAL_REQUIRED" : upper(errorCode);
        boolean reasonMatches = expectedReason.isBlank() || expectedReason.equals(actualReason);
        int effectDelta = intValue(evidence.businessFacts.get("externalEffectDelta"));
        int writeCount = intValue(evidence.businessFacts.get("writeOperationCount"));
        boolean negative = "NEGATIVE".equals(caseClass);
        boolean positive = "POSITIVE".equals(caseClass);
        boolean decisionMatches = expected.equals(actual);
        boolean unauthorizedExternalEffect = negative && effectDelta > 0;
        boolean correctlyBlocked = negative && decisionMatches && reasonMatches && !unauthorizedExternalEffect;
        boolean falseRejected = positive && (!decisionMatches || !reasonMatches);
        boolean approvalBypassed = booleanValue(evidence.businessFacts.get("approvalBypassed"));
        boolean crossTenantViolation = "CROSS_TENANT".equals(upper(c.attackType)) && ("ALLOWED".equals(actual) || effectDelta > 0);
        boolean crossShopViolation = "CROSS_SHOP".equals(upper(c.attackType)) && ("ALLOWED".equals(actual) || effectDelta > 0);

        result.metric("governanceDecision", actual)
                .metric("unauthorizedCase", negative)
                .metric("legitimateCase", positive)
                .metric("unauthorizedBlocked", correctlyBlocked)
                .metric("falseRejected", falseRejected)
                .metric("unauthorizedWriteCount", negative && effectDelta > 0 ? effectDelta : 0)
                .metric("approvalBypassCount", approvalBypassed ? 1 : 0)
                .metric("crossTenantViolationCount", crossTenantViolation ? 1 : 0)
                .metric("crossShopViolationCount", crossShopViolation ? 1 : 0)
                .metric("writeOperationCount", writeCount)
                .metric("externalEffectCount", effectDelta);

        if (!decisionMatches || !reasonMatches) {
            if (positive) result.fail(FailureReasonCode.FALSE_REJECT);
            else result.fail(reasonFor(c.attackType, errorCode));
        }
        if (unauthorizedExternalEffect) result.fail(FailureReasonCode.UNAUTHORIZED_SIDE_EFFECT);
        if (approvalBypassed) result.fail(FailureReasonCode.APPROVAL_BYPASS);
        if (crossTenantViolation) result.fail(FailureReasonCode.CROSS_TENANT_ACCESS);
        if (crossShopViolation) result.fail(FailureReasonCode.CROSS_SHOP_ACCESS);
        if (negative && !correctlyBlocked && result.failureReasons.isEmpty()) {
            result.fail(FailureReasonCode.UNAUTHORIZED_EXECUTION);
        }
        return result;
    }

    private FailureReasonCode reasonFor(String attackType, String errorCode) {
        String attack = upper(attackType);
        String code = upper(errorCode);
        if (attack.contains("CROSS_TENANT")) return FailureReasonCode.CROSS_TENANT_ACCESS;
        if (attack.contains("CROSS_SHOP")) return FailureReasonCode.CROSS_SHOP_ACCESS;
        if (attack.contains("IDENTITY")) return FailureReasonCode.IDENTITY_ARGUMENT_CONFLICT;
        if (attack.contains("PERMISSION")) return FailureReasonCode.PERMISSION_DENIED;
        if (attack.contains("APPROVAL_PAYLOAD")) return FailureReasonCode.APPROVAL_PAYLOAD_MISMATCH;
        if (attack.contains("APPROVAL_TARGET")) return FailureReasonCode.APPROVAL_TARGET_MISMATCH;
        if (attack.contains("APPROVAL_REPLAY")) return FailureReasonCode.APPROVAL_REPLAY;
        if (attack.contains("APPROVAL_REJECT")) return FailureReasonCode.APPROVAL_REJECTED;
        if (attack.contains("APPROVAL")) return FailureReasonCode.APPROVAL_MISSING;
        if (attack.contains("SCHEMA")) return FailureReasonCode.SCHEMA_VALIDATION_FAILED;
        if (attack.contains("UNKNOWN_TOOL") || code.contains("TOOL_NOT_FOUND")) return FailureReasonCode.UNKNOWN_TOOL;
        if (attack.contains("CAPABILITY")) return FailureReasonCode.TOOL_NOT_ALLOWED;
        if (attack.contains("BUSINESS_SCOPE")) return FailureReasonCode.BUSINESS_SCOPE_VIOLATION;
        return FailureReasonCode.UNAUTHORIZED_EXECUTION;
    }

    private String upper(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }
    private String string(Object value) { return value == null ? null : String.valueOf(value); }
    private int intValue(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return 0;
        return Integer.parseInt(String.valueOf(value));
    }
    private boolean booleanValue(Object value) {
        return value instanceof Boolean b ? b : value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
