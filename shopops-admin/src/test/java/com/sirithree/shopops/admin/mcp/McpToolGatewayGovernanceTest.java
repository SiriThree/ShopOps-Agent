package com.sirithree.shopops.admin.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.organization.service.ShopRuntimeConfigService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolProvider;
import com.sirithree.shopops.admin.tool.service.impl.DefaultToolGatewayService;
import com.sirithree.shopops.admin.tool.service.impl.ToolInputSchemaValidator;
import com.sirithree.shopops.admin.tool.service.impl.TrustedToolInputNormalizer;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class McpToolGatewayGovernanceTest {

    @Test
    void shouldRejectPermissionBeforeSelectingOrInvokingMcpProvider() {
        JacksonJsonSupport jsonSupport = new JacksonJsonSupport(new ObjectMapper());
        McpToolService toolService = mock(McpToolService.class);
        ToolCallLogService callLogService = mock(ToolCallLogService.class);
        TraceService traceService = mock(TraceService.class);
        ApprovalRequestService approvalService = mock(ApprovalRequestService.class);
        ShopRuntimeConfigService runtimeConfigService = mock(ShopRuntimeConfigService.class);
        ToolProvider provider = mock(ToolProvider.class);

        when(toolService.getTool(1L, CommerceMcpContracts.COMMENT_QUERY_NEGATIVE))
                .thenReturn(remoteTool());
        when(callLogService.start(any(), eq(CommerceMcpContracts.COMMENT_QUERY_NEGATIVE), any()))
                .thenReturn(9101L);
        when(traceService.startSpan(any())).thenReturn("span-permission-denied");

        DefaultToolGatewayService gateway = new DefaultToolGatewayService(
                toolService,
                callLogService,
                traceService,
                approvalService,
                runtimeConfigService,
                jsonSupport,
                new ToolInputSchemaValidator(jsonSupport),
                new TrustedToolInputNormalizer(jsonSupport),
                java.util.List.of(provider),
                "");

        ToolInvokeResult result = gateway.invoke(
                context(Set.of()),
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                Map.of(
                        "shopId", 1,
                        "startDate", "2026-08-01",
                        "endDate", "2026-08-03",
                        "minStar", 3));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("TOOL_PERMISSION_DENIED");
        verifyNoInteractions(provider);
    }

    private ToolInvokeContext context(Set<String> permissions) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(7L);
        context.setTaskId(1010L);
        context.setStepId(2010L);
        context.setTraceId("trace-phase8-permission");
        context.setPermissions(permissions);
        return context;
    }

    private McpToolDto remoteTool() {
        McpToolDto tool = new McpToolDto(
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                "Negative comment query",
                "comment",
                "comment:read",
                "LOW");
        tool.setInputSchema(CommerceMcpContracts.canonicalJson(
                CommerceMcpContracts.commentQueryNegativeInputSchema()));
        tool.setProviderType(CommerceMcpContracts.PROVIDER_MCP);
        tool.setMcpServerCode(CommerceMcpContracts.SERVER_CODE);
        tool.setRemoteToolName(CommerceMcpContracts.COMMENT_QUERY_NEGATIVE);
        tool.setSchemaHash(CommerceMcpContracts.commentQueryNegativeSchemaHash());
        tool.setDiscoveryStatus(CommerceMcpContracts.DISCOVERY_READY);
        tool.setEnabled(true);
        return tool;
    }
}
