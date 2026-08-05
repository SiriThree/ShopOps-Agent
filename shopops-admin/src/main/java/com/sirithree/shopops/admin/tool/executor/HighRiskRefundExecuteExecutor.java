package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import com.sirithree.shopops.admin.reliability.service.RefundExternalClient;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HighRiskRefundExecuteExecutor implements ToolExecutor {
    private final WriteOperationService operations;
    private final RefundExternalClient externalClient;

    public HighRiskRefundExecuteExecutor(WriteOperationService operations, RefundExternalClient externalClient) {
        this.operations = operations;
        this.externalClient = externalClient;
    }

    @Override
    public String toolCode() {
        return "order.refund_execute";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> values = ToolInputParser.asMap(input);
        String fallbackId = context.getApprovalId() == null ? "manual-" + context.getTraceId() : "approval-" + context.getApprovalId();
        String orderId = String.valueOf(values.getOrDefault("orderId", fallbackId));
        String requestId = String.valueOf(values.getOrDefault("operationRequestId", fallbackId));
        int amount = ToolInputParser.intValue(values, "refundAmount", 0);
        if (amount <= 0) {
            return ToolInvokeResult.failed("INVALID_REFUND_AMOUNT", "refundAmount must be greater than 0", null);
        }
        WriteOperation operation = operations.prepare(context, toolCode(), orderId, requestId, input);
        if (WriteOperationStatus.SUCCEEDED.equals(operation.getStatus())) {
            return ToolInvokeResult.success(response(operation, true, amount), null);
        }
        if (WriteOperationStatus.EXECUTING.equals(operation.getStatus()) && !operation.isFreshExecution()) {
            return ToolInvokeResult.failed("OPERATION_IN_PROGRESS", "Write operation is already executing", null);
        }
        if (WriteOperationStatus.EXTERNAL_UNKNOWN.equals(operation.getStatus())
                || WriteOperationStatus.NEEDS_RECONCILIATION.equals(operation.getStatus())) {
            return ToolInvokeResult.failed("EXTERNAL_RESULT_UNKNOWN", "External result is unknown; reconciliation is required", null);
        }
        RefundExternalClient.ExternalResult result = externalClient.execute(
                requestId,
                orderId,
                amount,
                String.valueOf(values.getOrDefault("simulation", "success"))
        );
        if ("UNKNOWN".equals(result.status())) {
            WriteOperation updated = operations.externalUnknown(operation, result.reference(), result.message());
            return ToolInvokeResult.failed("EXTERNAL_RESULT_UNKNOWN", "External result is unknown; operationId=" + updated.getId(), null);
        }
        if ("FAILED".equals(result.status())) {
            operations.failed(operation, "EXTERNAL_REJECTED", result.message());
            return ToolInvokeResult.failed("EXTERNAL_REJECTED", result.message(), null);
        }
        WriteOperation completed = operations.externalSucceeded(operation, result.reference(), result.data());
        return ToolInvokeResult.success(response(completed, false, amount), null);
    }

    private Map<String, Object> response(WriteOperation operation, boolean idempotentReplay, int refundAmount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operationId", operation.getId());
        data.put("status", outputStatus(operation.getStatus()));
        data.put("externalReference", operation.getExternalReference());
        data.put("idempotentReplay", idempotentReplay);
        data.put("approvalId", operation.getApprovalId());
        data.put("refundAmount", refundAmount);
        return data;
    }

    private String outputStatus(String status) {
        return WriteOperationStatus.SUCCEEDED.equals(status) ? "EXECUTED" : status;
    }
}
