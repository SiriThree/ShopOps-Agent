package com.sirithree.shopops.admin.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.mcp.client.OfficialCommerceMcpClient;
import com.sirithree.shopops.admin.mcp.config.CommerceMcpClientProperties;
import com.sirithree.shopops.admin.mcp.domain.McpClientException;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "shopops.mcp.integration.enabled", matches = "true")
class McpSchemaDriftExternalIntegrationTest {

    @Test
    void shouldInitializeAndListButRejectBeforeToolsCallWhenSchemaHashDrifts() {
        CommerceMcpClientProperties properties = new CommerceMcpClientProperties();
        properties.setEnabled(true);
        properties.setServerCode(CommerceMcpContracts.SERVER_CODE);
        properties.setEndpoint("/mcp");
        properties.setBaseUrl(System.getProperty("shopops.mcp.integration.base-url", "http://127.0.0.1:8090"));
        properties.setBearerToken(System.getProperty("shopops.mcp.integration.token", "phase8-test-token"));
        properties.setConnectTimeoutMs(2000);
        properties.setRequestTimeoutMs(5000);
        OfficialCommerceMcpClient client = new OfficialCommerceMcpClient(properties);

        McpClientException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> client.discoverAndCall(
                        context(),
                        CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                        Map.of(
                                "shopId", 1,
                                "startDate", "2026-08-01",
                                "endDate", "2026-08-03",
                                "minStar", 3),
                        "tampered-schema-hash"),
                McpClientException.class);

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode()).isEqualTo("MCP_TOOL_SCHEMA_MISMATCH");
    }

    private ToolInvokeContext context() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(7L);
        context.setTaskId(1002L);
        context.setStepId(2002L);
        context.setTraceId("trace-phase8-schema-drift");
        context.setPermissions(Set.of("comment:read"));
        return context;
    }
}