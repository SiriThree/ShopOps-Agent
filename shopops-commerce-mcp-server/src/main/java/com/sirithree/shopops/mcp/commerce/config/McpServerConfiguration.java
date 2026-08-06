package com.sirithree.shopops.mcp.commerce.config;

import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import com.sirithree.shopops.mcp.commerce.tool.CommentQueryNegativeMcpTool;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfiguration {

    @Bean
    public McpJsonMapper mcpJsonMapper() {
        return McpJsonDefaults.getMapper();
    }

    @Bean
    public HttpServletStreamableServerTransportProvider commerceMcpTransportProvider(
            McpJsonMapper jsonMapper,
            CommerceMcpServerProperties properties) {
        if (properties.getBearerToken() == null || properties.getBearerToken().isBlank()) {
            throw new IllegalStateException("SHOPOPS_COMMERCE_MCP_TOKEN must be configured");
        }
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .mcpEndpoint("/mcp")
                .contextExtractor(this::extractTrustedContext)
                .build();
    }

    @Bean
    public ServletRegistrationBean<?> commerceMcpServlet(
            HttpServletStreamableServerTransportProvider transportProvider) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration =
                new ServletRegistrationBean<>(transportProvider, "/mcp/*");
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean(destroyMethod = "close")
    public McpSyncServer commerceMcpServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            CommerceMcpServerProperties properties,
            CommentQueryNegativeMcpTool commentTool) {
        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo(properties.getServerName(), properties.getServerVersion())
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .build();
        server.addTool(commentTool.specification());
        return server;
    }

    private McpTransportContext extractTrustedContext(HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tenantId", requiredPositiveLong(request, CommerceMcpContracts.HEADER_TENANT_ID));
        metadata.put("shopId", requiredPositiveLong(request, CommerceMcpContracts.HEADER_SHOP_ID));
        metadata.put("userId", requiredPositiveLong(request, CommerceMcpContracts.HEADER_USER_ID));
        metadata.put("taskId", optionalPositiveLong(request, CommerceMcpContracts.HEADER_TASK_ID));
        metadata.put("stepId", optionalPositiveLong(request, CommerceMcpContracts.HEADER_STEP_ID));
        metadata.put("approvalId", optionalPositiveLong(request, CommerceMcpContracts.HEADER_APPROVAL_ID));
        metadata.put("traceId", requiredText(request, CommerceMcpContracts.HEADER_TRACE_ID));
        return McpTransportContext.create(metadata);
    }

    private long requiredPositiveLong(HttpServletRequest request, String header) {
        Long value = optionalPositiveLong(request, header);
        if (value == null) {
            throw new IllegalArgumentException("Missing trusted MCP header: " + header);
        }
        return value;
    }

    private Long optionalPositiveLong(HttpServletRequest request, String header) {
        String raw = request.getHeader(header);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw);
            if (value <= 0) {
                throw new IllegalArgumentException("Trusted MCP header must be positive: " + header);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid trusted MCP header: " + header, ex);
        }
    }

    private String requiredText(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing trusted MCP header: " + header);
        }
        return value;
    }
}
