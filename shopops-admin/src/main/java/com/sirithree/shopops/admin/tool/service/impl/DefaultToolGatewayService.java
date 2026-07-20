package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.approval.domain.ApprovalRequestCreateParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultToolGatewayService implements ToolGatewayService {
    private final McpToolService mcpToolService;
    private final ToolCallLogService toolCallLogService;
    private final TraceService traceService;
    private final ApprovalRequestService approvalRequestService;
    private final JacksonJsonSupport jsonSupport;
    private final Map<String, ToolExecutor> executors;
    private final String failCode;

    public DefaultToolGatewayService(McpToolService mcpToolService,
                                     ToolCallLogService toolCallLogService,
                                     TraceService traceService,
                                     ApprovalRequestService approvalRequestService,
                                     JacksonJsonSupport jsonSupport,
                                     List<ToolExecutor> executors,
                                     @Value("${shopops.tool.fail-code:}") String failCode) {
        this.mcpToolService = mcpToolService;
        this.toolCallLogService = toolCallLogService;
        this.traceService = traceService;
        this.approvalRequestService = approvalRequestService;
        this.jsonSupport = jsonSupport;
        this.executors = executors.stream().collect(Collectors.toMap(ToolExecutor::toolCode, Function.identity()));
        this.failCode = failCode;
    }

    @Override
    public ToolInvokeResult invoke(ToolInvokeContext context, String toolCode, Object input) {
        long started = System.currentTimeMillis();
        String spanId = startToolSpan(context, toolCode);
        Long logId = toolCallLogService.start(context, toolCode, input);
        try {
            if (toolCode.equals(failCode)) {
                return fail(context, spanId, logId, started, "TOOL_FAILURE_INJECTED", "Simulated tool failure: " + toolCode);
            }
            McpToolDto tool = mcpToolService.getTool(context.getTenantId(), toolCode);
            if (tool == null) {
                return fail(context, spanId, logId, started, "TOOL_NOT_FOUND", "工具不存在: " + toolCode);
            }
            if (!Boolean.TRUE.equals(tool.getEnabled())) {
                return fail(context, spanId, logId, started, "TOOL_DISABLED", "工具已停用: " + toolCode);
            }
            if (Boolean.TRUE.equals(tool.getNeedApproval())) {
                return approvalRequired(context, spanId, logId, started, tool, input);
            }
            ToolExecutor executor = executors.get(toolCode);
            if (executor == null) {
                return fail(context, spanId, logId, started, "EXECUTOR_NOT_FOUND", "未注册工具执行器: " + toolCode);
            }
            ToolInvokeResult result = executor.execute(context, input);
            if (Boolean.TRUE.equals(result.getSuccess())) {
                toolCallLogService.success(logId, result.getData(), System.currentTimeMillis() - started);
                traceService.finishSpan(context.getTraceId(), spanId, "SUCCESS", "工具调用成功: " + toolCode, null);
                result.setToolCallLogId(logId);
                return result;
            }
            toolCallLogService.failed(logId, result.getErrorCode(), result.getErrorMessage(), System.currentTimeMillis() - started);
            traceService.finishSpan(context.getTraceId(), spanId, "FAILED", null, result.getErrorMessage());
            result.setToolCallLogId(logId);
            return result;
        } catch (RuntimeException ex) {
            traceService.finishSpan(context.getTraceId(), spanId, "FAILED", null, ex.getMessage());
            return fail(logId, started, "TOOL_EXECUTE_ERROR", ex.getMessage());
        }
    }

    private ToolInvokeResult fail(Long logId, long started, String errorCode, String errorMessage) {
        toolCallLogService.failed(logId, errorCode, errorMessage, System.currentTimeMillis() - started);
        return ToolInvokeResult.failed(errorCode, errorMessage, logId);
    }

    private ToolInvokeResult fail(ToolInvokeContext context, String spanId, Long logId, long started, String errorCode, String errorMessage) {
        traceService.finishSpan(context.getTraceId(), spanId, "FAILED", null, errorMessage);
        return fail(logId, started, errorCode, errorMessage);
    }

    private ToolInvokeResult approvalRequired(ToolInvokeContext context, String spanId, Long logId, long started,
                                              McpToolDto tool, Object input) {
        ApprovalRequestDto approval = approvalRequestService.create(
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                requesterName(context),
                approvalParam(context, logId, tool, input)
        );
        String riskLevel = normalizedRiskLevel(tool.getRiskLevel());
        String message = "工具调用需要审批: " + tool.getToolCode();
        toolCallLogService.approvalRequired(logId, approval.getApprovalId(), riskLevel, message,
                System.currentTimeMillis() - started);
        traceService.finishSpan(context.getTraceId(), spanId, "APPROVAL_REQUIRED",
                "approvalId=" + approval.getApprovalId(), message);
        return ToolInvokeResult.approvalRequired(logId, approval.getApprovalId(), Map.of(
                "approvalId", approval.getApprovalId(),
                "approvalNo", approval.getApprovalNo(),
                "toolCode", tool.getToolCode(),
                "riskLevel", riskLevel,
                "status", approval.getStatus()
        ));
    }

    private ApprovalRequestCreateParam approvalParam(ToolInvokeContext context, Long logId, McpToolDto tool, Object input) {
        ApprovalRequestCreateParam param = new ApprovalRequestCreateParam();
        param.setSourceType("TOOL_CALL");
        param.setSourceId(logId);
        param.setTaskId(context.getTaskId());
        param.setStepId(context.getStepId());
        param.setTraceId(context.getTraceId());
        param.setToolCode(tool.getToolCode());
        param.setRiskLevel(normalizedRiskLevel(tool.getRiskLevel()));
        param.setTitle("审批工具调用: " + tool.getToolName());
        param.setReason("工具风险等级或注册元数据要求人工审批");
        param.setInputSummary(jsonSupport.toJson(input));
        return param;
    }

    private String requesterName(ToolInvokeContext context) {
        return context.getUserId() == null ? "system" : "user-" + context.getUserId();
    }

    private String normalizedRiskLevel(String riskLevel) {
        return riskLevel == null || riskLevel.isBlank() ? "UNKNOWN" : riskLevel.toUpperCase();
    }

    private String startToolSpan(ToolInvokeContext context, String toolCode) {
        TraceSpanCreateCommand command = new TraceSpanCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setTraceId(context.getTraceId());
        command.setParentSpanId(context.getParentSpanId());
        command.setSpanType("tool");
        command.setSpanName("tool." + toolCode);
        command.setRefType("tool");
        command.setRefId(context.getStepId());
        command.setInputSummary("invoke " + toolCode);
        return traceService.startSpan(command);
    }
}
