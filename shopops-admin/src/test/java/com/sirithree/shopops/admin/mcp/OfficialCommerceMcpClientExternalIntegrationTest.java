package com.sirithree.shopops.admin.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.mcp.client.OfficialCommerceMcpClient;
import com.sirithree.shopops.admin.mcp.config.CommerceMcpClientProperties;
import com.sirithree.shopops.admin.mcp.provider.McpToolProvider;
import com.sirithree.shopops.admin.organization.service.ShopRuntimeConfigService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.impl.DefaultToolGatewayService;
import com.sirithree.shopops.admin.tool.service.impl.ToolInputSchemaValidator;
import com.sirithree.shopops.admin.tool.service.impl.TrustedToolInputNormalizer;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "shopops.mcp.integration.enabled", matches = "true")
class OfficialCommerceMcpClientExternalIntegrationTest {

    @Test
    void shouldExecuteThroughGovernedGatewayWithoutAnyLocalExecutor() {
        JacksonJsonSupport jsonSupport =
                new JacksonJsonSupport(new ObjectMapper());

        OfficialCommerceMcpClient client =
                new OfficialCommerceMcpClient(properties());

        McpToolProvider mcpProvider =
                new McpToolProvider(client, jsonSupport);

        McpToolService toolService = mock(McpToolService.class);
        ToolCallLogService callLogService = mock(ToolCallLogService.class);
        TraceService traceService = mock(TraceService.class);
        ApprovalRequestService approvalService =
                mock(ApprovalRequestService.class);
        ShopRuntimeConfigService runtimeConfigService =
                mock(ShopRuntimeConfigService.class);

        when(toolService.getTool(
                1L,
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE))
                .thenReturn(remoteTool());

        when(callLogService.start(
                any(),
                eq(CommerceMcpContracts.COMMENT_QUERY_NEGATIVE),
                any()))
                .thenReturn(9001L);

        when(traceService.startSpan(any()))
                .thenReturn("span-phase8-mcp");

        when(runtimeConfigService.value(
                anyLong(),
                anyLong(),
                eq("agent_tool_approval_enabled")))
                .thenReturn(Optional.empty());

        DefaultToolGatewayService gateway =
                new DefaultToolGatewayService(
                        toolService,
                        callLogService,
                        traceService,
                        approvalService,
                        runtimeConfigService,
                        jsonSupport,
                        new ToolInputSchemaValidator(jsonSupport),
                        new TrustedToolInputNormalizer(jsonSupport),
                        // Deliberately omit LocalToolProvider and every ToolExecutor.
                        java.util.List.of(mcpProvider),
                        "");

        ToolInvokeResult result = gateway.invoke(
                context(Set.of("comment:read")),
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                Map.of(
                        "shopId", 1,
                        "startDate", "2026-08-01",
                        "endDate", "2026-08-03",
                        "minStar", 3));

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isInstanceOf(Map.class);

        Map<?, ?> data = (Map<?, ?>) result.getData();

        Object negativeCountValue = data.get("negativeCount");
        assertThat(negativeCountValue).isInstanceOf(Number.class);
        assertThat(((Number) negativeCountValue).longValue())
                .isEqualTo(3L);

        assertThat(data.get("_mcp")).isNotNull();

        verify(callLogService)
                .success(eq(9001L), any(), anyLong());

        verify(traceService)
                .finishSpan(
                        eq("trace-phase8-admin-client"),
                        eq("span-phase8-mcp"),
                        eq("SUCCESS"),
                        any(),
                        isNull());
    }


    private CommerceMcpClientProperties properties() {
        CommerceMcpClientProperties properties = new CommerceMcpClientProperties();
        properties.setEnabled(true);
        properties.setServerCode(CommerceMcpContracts.SERVER_CODE);
        properties.setBaseUrl(System.getProperty("shopops.mcp.integration.base-url", "http://127.0.0.1:8090"));
        properties.setEndpoint("/mcp");
        properties.setBearerToken(System.getProperty("shopops.mcp.integration.token", "phase8-test-token"));
        properties.setConnectTimeoutMs(2000);
        properties.setRequestTimeoutMs(5000);
        return properties;
    }

    private ToolInvokeContext context(Set<String> permissions) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(7L);
        context.setTaskId(1001L);
        context.setStepId(2001L);
        context.setTraceId("trace-phase8-admin-client");
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
        tool.setDescription("Query negative comments through an independent MCP server");
        tool.setInputSchema(CommerceMcpContracts.canonicalJson(
                CommerceMcpContracts.commentQueryNegativeInputSchema()));
        tool.setOutputSchema(CommerceMcpContracts.canonicalJson(
                CommerceMcpContracts.commentQueryNegativeOutputSchema()));
        tool.setNeedApproval(false);
        tool.setIdempotent(true);
        tool.setProviderType(CommerceMcpContracts.PROVIDER_MCP);
        tool.setMcpServerCode(CommerceMcpContracts.SERVER_CODE);
        tool.setRemoteToolName(CommerceMcpContracts.COMMENT_QUERY_NEGATIVE);
        tool.setSchemaHash(CommerceMcpContracts.commentQueryNegativeSchemaHash());
        tool.setDiscoveryStatus(CommerceMcpContracts.DISCOVERY_READY);
        return tool;
    }
}