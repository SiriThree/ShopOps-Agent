package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.approval.domain.ApprovalStatus;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import java.util.Set;

/** Stage-6 test-only classifier for distinguishing pre-idempotency governance blocks from idempotency decisions. */
public final class IdempotencyAttributionClassifier {
    private static final Set<String> AUTH_ERRORS = Set.of(
            "TOOL_AUTHORIZATION_DENIED", "TOOL_PERMISSION_SNAPSHOT_MISMATCH", "TOOL_PERMISSION_DENIED",
            "TOOL_TRUSTED_CONTEXT_MISSING", "TOOL_AUTHORIZATION_ARGUMENT_FORBIDDEN");
    private static final Set<String> SCHEMA_ERRORS = Set.of("MCP_INPUT_INVALID");
    private static final Set<String> SCOPE_ERRORS = Set.of(
            "BUSINESS_SCOPE_VIOLATION", "TOOL_SCOPE_INVALID", "TOOL_SCOPE_ARGUMENT_CONFLICT",
            "TOOL_IDENTITY_ARGUMENT_CONFLICT");
    private static final Set<String> APPROVAL_ERRORS = Set.of("APPROVAL_NOT_APPROVED", "APPROVAL_EXECUTION_CONFLICT");
    private static final Set<String> BOUNDARY_ERRORS = Set.of(
            "IDEMPOTENCY_PAYLOAD_MISMATCH", "OPERATION_IN_PROGRESS", "RECOVERY_REQUIRED",
            "MANUAL_REVIEW_REQUIRED", "OPERATION_FAILED_TERMINAL", "EXTERNAL_RESULT_UNKNOWN", "EXTERNAL_REJECTED");

    private IdempotencyAttributionClassifier() {}

    public static IdempotencyAttemptAttribution classify(
            int attemptNo,
            String attemptKind,
            boolean intendedReplay,
            Long approvalId,
            String approvalStatus,
            ToolInvokeResult result,
            boolean writeOperationExistsAfter,
            boolean externalAttemptObserved) {
        String error = result == null || result.getErrorCode() == null ? "" : result.getErrorCode();
        boolean success = result != null && Boolean.TRUE.equals(result.getSuccess());
        boolean authPassed = !AUTH_ERRORS.contains(error);
        boolean schemaPassed = authPassed && !SCHEMA_ERRORS.contains(error);
        boolean scopePassed = schemaPassed && !SCOPE_ERRORS.contains(error);
        boolean approvalPassed = scopePassed && !APPROVAL_ERRORS.contains(error)
                && (ApprovalStatus.EXECUTING.equals(approvalStatus)
                || ApprovalStatus.EXECUTED.equals(approvalStatus)
                || ApprovalStatus.EXECUTION_FAILED.equals(approvalStatus));
        boolean boundaryReached = approvalPassed && (success || BOUNDARY_ERRORS.contains(error)
                || ("TOOL_EXECUTE_ERROR".equals(error) && writeOperationExistsAfter));
        boolean preBlocked = !boundaryReached;
        String code;
        if (boundaryReached) {
            if ("IDEMPOTENCY_PAYLOAD_MISMATCH".equals(error)) code = "IDEMPOTENCY_PAYLOAD_CONFLICT";
            else if (success && result.getData() instanceof java.util.Map<?, ?> data
                    && Boolean.TRUE.equals(data.get("idempotentReplay"))) code = "IDEMPOTENCY_REPLAY_DEDUPED";
            else if ("OPERATION_IN_PROGRESS".equals(error)) code = "IDEMPOTENCY_CONCURRENT_WINNER";
            else code = "IDEMPOTENCY_BOUNDARY_REACHED";
        } else if (AUTH_ERRORS.contains(error)) code = "ATTRIBUTION_INVALID_AUTHORIZATION_BLOCK";
        else if (SCHEMA_ERRORS.contains(error)) code = "ATTRIBUTION_INVALID_SCHEMA_BLOCK";
        else if (SCOPE_ERRORS.contains(error)) code = "ATTRIBUTION_INVALID_SCOPE_BLOCK";
        else if (APPROVAL_ERRORS.contains(error)) code = "ATTRIBUTION_INVALID_APPROVAL_BLOCK";
        else code = "ATTRIBUTION_INVALID_PRE_IDEMPOTENCY_BLOCK";
        return new IdempotencyAttemptAttribution(
                attemptNo, attemptKind, intendedReplay, approvalId, true,
                authPassed, schemaPassed, scopePassed, approvalPassed,
                boundaryReached, boundaryReached, externalAttemptObserved, preBlocked,
                code, result == null ? "" : result.getStatus(), error);
    }
}
