package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.approval.domain.ApprovalRequestCreateParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalStatus;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.organization.service.ShopRuntimeConfigService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolGovernanceException;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolProvider;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultToolGatewayService implements ToolGatewayService {
    private final McpToolService mcpToolService;
    private final ToolCallLogService toolCallLogService;
    private final TraceService traceService;
    private final ApprovalRequestService approvalRequestService;
    private final ShopRuntimeConfigService shopRuntimeConfigService;
    private final JacksonJsonSupport jsonSupport;
    private final ToolInputSchemaValidator toolInputSchemaValidator;
    private final TrustedToolInputNormalizer trustedToolInputNormalizer;
    private final List<ToolProvider> toolProviders;
    private final String failCode;

    public DefaultToolGatewayService(McpToolService mcpToolService,
                                     ToolCallLogService toolCallLogService,
                                     TraceService traceService,
                                     ApprovalRequestService approvalRequestService,
                                     ShopRuntimeConfigService shopRuntimeConfigService,
                                     JacksonJsonSupport jsonSupport,
                                     ToolInputSchemaValidator toolInputSchemaValidator,
                                     TrustedToolInputNormalizer trustedToolInputNormalizer,
                                     List<ToolProvider> toolProviders,
                                     @Value("${shopops.tool.fail-code:}") String failCode) {
        this.mcpToolService = mcpToolService;
        this.toolCallLogService = toolCallLogService;
        this.traceService = traceService;
        this.approvalRequestService = approvalRequestService;
        this.shopRuntimeConfigService = shopRuntimeConfigService;
        this.jsonSupport = jsonSupport;
        this.toolInputSchemaValidator = toolInputSchemaValidator;
        this.trustedToolInputNormalizer = trustedToolInputNormalizer;
        this.toolProviders = List.copyOf(toolProviders);
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
            Object normalizedInput = trustedToolInputNormalizer.normalize(context, tool, input);
            toolInputSchemaValidator.validate(tool, normalizedInput);
            if (tool.getPermissionCode() == null || tool.getPermissionCode().isBlank()
                    || !context.hasPermission(tool.getPermissionCode())) {
                return fail(context, spanId, logId, started, "TOOL_PERMISSION_DENIED",
                        "当前主体无权执行工具: " + toolCode);
            }
            boolean toolNeedsApproval = Boolean.TRUE.equals(tool.getNeedApproval());
            boolean toolApprovalEnabled = approvalEnabled(context);
            boolean approvalBypassedByShopConfig = toolNeedsApproval && !toolApprovalEnabled;
            if (toolNeedsApproval && toolApprovalEnabled) {
                if (context.getApprovalId() == null) {
                    return approvalRequired(context, spanId, logId, started, tool, normalizedInput);
                }
                if (!isApprovedForTool(context, toolCode, normalizedInput)) {
                    return fail(context, spanId, logId, started, "APPROVAL_NOT_APPROVED", "审批单不存在、未通过或不匹配工具: " + context.getApprovalId());
                }
            }
            List<ToolProvider> matchingProviders = toolProviders.stream()
                    .filter(provider -> provider.supports(tool))
                    .toList();
            if (matchingProviders.isEmpty()) {
                return fail(context, spanId, logId, started, "TOOL_PROVIDER_NOT_FOUND",
                        "未注册工具 Provider: " + toolCode + " providerType=" + tool.getProviderType());
            }
            if (matchingProviders.size() > 1) {
                return fail(context, spanId, logId, started, "TOOL_PROVIDER_CONFLICT",
                        "多个 ToolProvider 同时匹配: " + toolCode);
            }
            ToolProvider provider = matchingProviders.get(0);
            boolean approvalExecutionStarted = false;
            if (context.getApprovalId() != null) {
                ApprovalRequestDto approval = approvalRequestService.get(context.getTenantId(), context.getShopId(), context.getApprovalId()).orElse(null);
                if (approval != null && ApprovalStatus.APPROVED.equals(approval.getStatus())) {
                    approvalExecutionStarted = approvalRequestService.markExecuting(context.getTenantId(), context.getShopId(), context.getApprovalId());
                    if (!approvalExecutionStarted) {
                        return fail(context, spanId, logId, started, "APPROVAL_EXECUTION_CONFLICT", "审批已被并发执行");
                    }
                }
            }
            ToolInvokeResult result = provider.invoke(context, tool, normalizedInput);
            if (Boolean.TRUE.equals(result.getSuccess())) {
                if (context.getApprovalId() != null && approvalExecutionStarted) {
                    approvalRequestService.markExecuted(context.getTenantId(), context.getShopId(), context.getApprovalId());
                }
                if (approvalBypassedByShopConfig) {
                    toolCallLogService.successWithGovernanceNote(logId, result.getData(), normalizedRiskLevel(tool.getRiskLevel()),
                            "APPROVAL_BYPASSED_BY_SHOP_CONFIG",
                            "agent_tool_approval_enabled=false; approval was bypassed by shop runtime config",
                            System.currentTimeMillis() - started);
                } else {
                    toolCallLogService.success(logId, result.getData(), System.currentTimeMillis() - started);
                }
                traceService.finishSpan(context.getTraceId(), spanId, "SUCCESS", "工具调用成功: " + toolCode, null);
                result.setToolCallLogId(logId);
                result.setApprovalId(context.getApprovalId());
                return result;
            }
            if (context.getApprovalId() != null && approvalExecutionStarted) {
                approvalRequestService.markExecutionFailed(context.getTenantId(), context.getShopId(), context.getApprovalId(), result.getErrorMessage());
            }
            toolCallLogService.failed(logId, result.getErrorCode(), result.getErrorMessage(), System.currentTimeMillis() - started);
            traceService.finishSpan(context.getTraceId(), spanId, "FAILED", null, result.getErrorMessage());
            result.setToolCallLogId(logId);
            result.setApprovalId(context.getApprovalId());
            return result;
        } catch (ToolGovernanceException ex) {
            return fail(context, spanId, logId, started, ex.getErrorCode(), ex.getMessage());
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
        String canonicalInput = canonicalInputJson(input);
        param.setInputSummary(canonicalInput);
        param.setInputHash(sha256(canonicalInput));
        param.setBusinessObjectId(businessObjectId(input));
        return param;
    }

    private boolean isApprovedForTool(ToolInvokeContext context, String toolCode, Object input) {
        return approvalRequestService.get(context.getTenantId(), context.getShopId(), context.getApprovalId())
                .filter(approval -> ApprovalStatus.APPROVED.equals(approval.getStatus()) || ApprovalStatus.EXECUTING.equals(approval.getStatus()) || ApprovalStatus.EXECUTED.equals(approval.getStatus()))
                .filter(approval -> toolCode.equals(approval.getToolCode()))
                .filter(approval -> canonicalInputJson(input).equals(approval.getInputSummary()))
                .isPresent();
    }

    private String canonicalInputJson(Object input) {
        Map<String, Object> values = new java.util.TreeMap<>(jsonSupport.toMap(jsonSupport.toJson(input)));
        values.remove("approvalId");
        return jsonSupport.toJson(values);
    }

    private String businessObjectId(Object input) {
        Map<String, Object> values = jsonSupport.toMap(jsonSupport.toJson(input));
        Object value = values.get("orderId");
        if (value == null) value = values.get("productId");
        if (value == null) value = values.get("reportId");
        return value == null ? "unspecified" : String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private boolean approvalEnabled(ToolInvokeContext context) {
        return shopRuntimeConfigService.booleanValue(
                context.getTenantId(),
                context.getShopId(),
                "agent_tool_approval_enabled",
                true
        );
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
