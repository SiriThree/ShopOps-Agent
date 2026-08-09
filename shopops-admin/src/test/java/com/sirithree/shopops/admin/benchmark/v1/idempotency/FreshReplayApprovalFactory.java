package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.Map;
import java.util.Set;

/**
 * Test-infrastructure helper that creates a fresh, normally-bound approval for every intended replay.
 * It never mutates approval state directly and never bypasses ToolGateway governance.
 */
public final class FreshReplayApprovalFactory {
    private final ToolGatewayService toolGateway;
    private final ApprovalRequestService approvals;

    public FreshReplayApprovalFactory(ToolGatewayService toolGateway, ApprovalRequestService approvals) {
        this.toolGateway = toolGateway;
        this.approvals = approvals;
    }

    public ReplayApproval createApproved(long tenantId, long shopId, long userId, long taskId,
                                         String traceId, Map<String, Object> input) {
        ToolInvokeContext context = context(tenantId, shopId, userId, taskId, traceId, null);
        ToolInvokeResult pending = toolGateway.invoke(context, "order.refund_execute", input);
        if (!"APPROVAL_REQUIRED".equals(pending.getStatus()) || pending.getApprovalId() == null) {
            throw new IllegalStateException("Expected approval before refund execution but got " + pending.getStatus());
        }
        ApprovalDecisionParam decision = new ApprovalDecisionParam();
        decision.setComment("ShopOpsBench Stage 6 fresh replay approval");
        decision.setConfirmText("确认通过");
        ApprovalRequestDto approved = approvals.approve(
                        tenantId, shopId, pending.getApprovalId(), userId, "benchmark-approver", decision)
                .orElseThrow(() -> new IllegalStateException("Approval transition failed: " + pending.getApprovalId()));
        return new ReplayApproval(
                approved.getApprovalId(), approved.getInputHash(), approved.getInputSummary(),
                approved.getBusinessObjectId(), approved.getToolCode(), approved.getStatus());
    }

    public ToolInvokeContext context(long tenantId, long shopId, long userId, long taskId,
                                     String traceId, Long approvalId) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(tenantId);
        context.setShopId(shopId);
        context.setUserId(userId);
        context.setTaskId(taskId);
        context.setTraceId(traceId);
        context.setApprovalId(approvalId);
        context.setPermissions(Set.of("order:read", "order:refund"));
        context.setManualInvoke(true);
        return context;
    }

    public record ReplayApproval(
            Long approvalId,
            String inputHash,
            String inputSummary,
            String businessObjectId,
            String toolCode,
            String status) {
    }
}
