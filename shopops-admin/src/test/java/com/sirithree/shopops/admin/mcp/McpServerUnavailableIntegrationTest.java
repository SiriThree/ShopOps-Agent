package com.sirithree.shopops.admin.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.mcp.client.OfficialCommerceMcpClient;
import com.sirithree.shopops.admin.mcp.config.CommerceMcpClientProperties;
import com.sirithree.shopops.admin.mcp.domain.McpClientException;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.Set;
import org.junit.jupiter.api.Test;

class McpServerUnavailableIntegrationTest {

    @Test
    void shouldFailClearlyWhenMcpServerIsNotRunning() {
        CommerceMcpClientProperties properties =
                new CommerceMcpClientProperties();

        // 必须先启用，否则会在连接前返回 MCP_SERVER_DISABLED
        properties.setEnabled(true);

        properties.setServerCode("commerce-unavailable");
        properties.setBaseUrl("http://127.0.0.1:9");
        properties.setEndpoint("/mcp");
        properties.setBearerToken("unused-test-token");
        properties.setConnectTimeoutMs(300);
        properties.setRequestTimeoutMs(300);

        OfficialCommerceMcpClient client =
                new OfficialCommerceMcpClient(properties);

        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(7L);
        context.setTaskId(1001L);
        context.setStepId(2001L);
        context.setTraceId("trace-server-unavailable");
        context.setPermissions(Set.of("comment:read"));

        McpClientException error =
                org.assertj.core.api.Assertions.catchThrowableOfType(
                        () -> client.discover(context),
                        McpClientException.class);

        assertThat(error).isNotNull();
        assertThat(error.getErrorCode())
                .isEqualTo("MCP_CONNECT_FAILED");
    }
}
