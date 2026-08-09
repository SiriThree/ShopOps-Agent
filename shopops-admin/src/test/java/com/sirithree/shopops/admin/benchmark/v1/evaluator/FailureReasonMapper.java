package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import java.util.Map;

public final class FailureReasonMapper {
    private FailureReasonMapper() {}

    public static FailureReasonCode fromToolLog(Map<String, Object> log) {
        String errorCode = string(log == null ? null : log.get("errorCode"));
        if ("TOOL_PERMISSION_DENIED".equals(errorCode)) return FailureReasonCode.UNAUTHORIZED_EXECUTION;
        if ("APPROVAL_REQUIRED".equals(errorCode)) return FailureReasonCode.APPROVAL_REQUIRED;
        if ("APPROVAL_NOT_APPROVED".equals(errorCode) || "APPROVAL_EXECUTION_CONFLICT".equals(errorCode)) {
            return FailureReasonCode.APPROVAL_REQUIRED;
        }
        if ("APPROVAL_BYPASSED_BY_SHOP_CONFIG".equals(errorCode)) return FailureReasonCode.APPROVAL_BYPASS;
        if (errorCode != null && (errorCode.contains("SCHEMA")
                || errorCode.contains("ARGUMENT")
                || errorCode.contains("INPUT_INVALID")
                || errorCode.contains("SCOPE_INVALID")
                || errorCode.contains("SCOPE_MISMATCH")
                || errorCode.contains("TRUSTED_CONTEXT_MISSING"))) {
            return FailureReasonCode.INVALID_TOOL_ARGUMENT;
        }
        return FailureReasonCode.TOOL_EXECUTION_ERROR;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
