package com.sirithree.shopops.admin.mcp.client;

import com.sirithree.shopops.admin.mcp.config.CommerceMcpClientProperties;
import com.sirithree.shopops.admin.mcp.domain.McpClientException;
import com.sirithree.shopops.admin.mcp.domain.McpDiscoveredTool;
import com.sirithree.shopops.admin.mcp.domain.McpDiscoveryResult;
import com.sirithree.shopops.admin.mcp.domain.McpRemoteCallResult;
import com.sirithree.shopops.admin.mcp.domain.McpVerifiedCallResult;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OfficialCommerceMcpClient implements CommerceMcpClient {

    private static final Logger log =
            LoggerFactory.getLogger(OfficialCommerceMcpClient.class);

    private final CommerceMcpClientProperties properties;

    public OfficialCommerceMcpClient(
            CommerceMcpClientProperties properties) {
        this.properties = properties;
    }

    @Override
    public McpDiscoveryResult discover(
            ToolInvokeContext context) {

        return withInitializedClient(
                context,
                (client, initializeResult) -> {
                    ListToolsResult listToolsResult =
                            client.listTools();

                    protocolEvidence(
                            context,
                            "tools/list",
                            initializeResult.protocolVersion(),
                            null);

                    List<McpDiscoveredTool> tools =
                            listToolsResult.tools()
                                    .stream()
                                    .map(tool ->
                                            discoveredTool(
                                                    tool,
                                                    initializeResult))
                                    .toList();

                    return new McpDiscoveryResult(
                            properties.getServerCode(),
                            initializeResult.protocolVersion(),
                            initializeResult.serverInfo().name(),
                            initializeResult.serverInfo().version(),
                            tools);
                });
    }

    @Override
    public McpRemoteCallResult call(
            ToolInvokeContext context,
            String remoteToolName,
            Map<String, Object> arguments) {

        return withInitializedClient(
                context,
                (client, initializeResult) -> {
                    CallToolResult result =
                            client.callTool(
                                    CallToolRequest
                                            .builder(remoteToolName)
                                            .arguments(arguments)
                                            .build());

                    protocolEvidence(
                            context,
                            "tools/call",
                            initializeResult.protocolVersion(),
                            remoteToolName);

                    return new McpRemoteCallResult(
                            properties.getServerCode(),
                            remoteToolName,
                            initializeResult.protocolVersion(),
                            Boolean.TRUE.equals(result.isError()),
                            structuredContent(result),
                            textContent(result));
                });
    }

    @Override
    public McpVerifiedCallResult discoverAndCall(
            ToolInvokeContext context,
            String remoteToolName,
            Map<String, Object> arguments,
            String expectedSchemaHash) {

        return withInitializedClient(
                context,
                (client, initializeResult) -> {
                    ListToolsResult listToolsResult =
                            client.listTools();

                    protocolEvidence(
                            context,
                            "tools/list",
                            initializeResult.protocolVersion(),
                            null);

                    Tool remoteTool =
                            listToolsResult.tools()
                                    .stream()
                                    .filter(tool ->
                                            remoteToolName.equals(
                                                    tool.name()))
                                    .findFirst()
                                    .orElseThrow(() ->
                                            new McpClientException(
                                                    "MCP_TOOL_NOT_DISCOVERED",
                                                    "Remote MCP tool was not discovered: "
                                                            + remoteToolName));

                    McpDiscoveredTool discovered =
                            discoveredTool(
                                    remoteTool,
                                    initializeResult);

                    if (expectedSchemaHash == null
                            || expectedSchemaHash.isBlank()) {
                        throw new McpClientException(
                                "MCP_SCHEMA_HASH_MISSING",
                                "Local governance metadata does not contain "
                                        + "an approved schema hash");
                    }

                    if (!expectedSchemaHash.equals(
                            discovered.schemaHash())) {
                        throw new McpClientException(
                                "MCP_TOOL_SCHEMA_MISMATCH",
                                "Remote schema hash changed for tool: "
                                        + remoteToolName);
                    }

                    CallToolResult result =
                            client.callTool(
                                    CallToolRequest
                                            .builder(remoteToolName)
                                            .arguments(arguments)
                                            .build());

                    protocolEvidence(
                            context,
                            "tools/call",
                            initializeResult.protocolVersion(),
                            remoteToolName);

                    McpRemoteCallResult callResult =
                            new McpRemoteCallResult(
                                    properties.getServerCode(),
                                    remoteToolName,
                                    initializeResult.protocolVersion(),
                                    Boolean.TRUE.equals(
                                            result.isError()),
                                    structuredContent(result),
                                    textContent(result));

                    return new McpVerifiedCallResult(
                            discovered,
                            callResult);
                });
    }

    private <T> T withInitializedClient(
            ToolInvokeContext context,
            ClientOperation<T> operation) {

        validateConfiguration();
        validateTrustedContext(context);

        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport
                        .builder(properties.getBaseUrl())
                        .endpoint(properties.getEndpoint())
                        .customizeClient(builder ->
                                builder.connectTimeout(
                                        Duration.ofMillis(
                                                properties
                                                        .getConnectTimeoutMs())))
                        .httpRequestCustomizer(
                                (
                                        request,
                                        method,
                                        endpoint,
                                        body,
                                        transportContext
                                ) -> {
                                    addTrustedHeaders(
                                            request,
                                            context);
                                })
                        .build();

        McpSyncClient client =
                McpClient.sync(transport)
                        .requestTimeout(
                                Duration.ofMillis(
                                        properties
                                                .getRequestTimeoutMs()))
                        .build();

        try {
            InitializeResult initializeResult =
                    client.initialize();

            protocolEvidence(
                    context,
                    "initialize",
                    initializeResult.protocolVersion(),
                    null);

            return operation.execute(
                    client,
                    initializeResult);
        }
        catch (McpClientException ex) {
            // Business and governance exceptions must retain their original error code.
            throw ex;
        }
        catch (RuntimeException ex) {
            // Only SDK, network, protocol, timeout and transport failures are remapped.
            throw mapException(ex);
        }
        finally {
            try {
                client.closeGracefully();
            }
            catch (RuntimeException closeError) {
                log.debug(
                        "MCP client close failed for serverCode={}: {}",
                        properties.getServerCode(),
                        closeError.getMessage());
            }
        }
    }

    private void addTrustedHeaders(
            java.net.http.HttpRequest.Builder request,
            ToolInvokeContext context) {

        request.header(
                "Authorization",
                "Bearer " + properties.getBearerToken());

        header(
                request,
                CommerceMcpContracts.HEADER_TENANT_ID,
                context.getTenantId());

        header(
                request,
                CommerceMcpContracts.HEADER_SHOP_ID,
                context.getShopId());

        header(
                request,
                CommerceMcpContracts.HEADER_USER_ID,
                context.getUserId());

        header(
                request,
                CommerceMcpContracts.HEADER_TASK_ID,
                context.getTaskId());

        header(
                request,
                CommerceMcpContracts.HEADER_STEP_ID,
                context.getStepId());

        header(
                request,
                CommerceMcpContracts.HEADER_TRACE_ID,
                context.getTraceId());

        header(
                request,
                CommerceMcpContracts.HEADER_APPROVAL_ID,
                context.getApprovalId());
    }

    private void header(
            java.net.http.HttpRequest.Builder request,
            String name,
            Object value) {

        if (value != null) {
            request.header(
                    name,
                    String.valueOf(value));
        }
    }

    private McpDiscoveredTool discoveredTool(
            Tool tool,
            InitializeResult initializeResult) {

        Map<String, Object> schema =
                tool.inputSchema() == null
                        ? Map.of()
                        : tool.inputSchema();

        return new McpDiscoveredTool(
                tool.name(),
                tool.description(),
                schema,
                CommerceMcpContracts.sha256(
                        CommerceMcpContracts.canonicalJson(
                                schema)),
                initializeResult.protocolVersion(),
                initializeResult.serverInfo().name(),
                initializeResult.serverInfo().version());
    }

    private Map<String, Object> structuredContent(
            CallToolResult result) {

        Object content =
                result.structuredContent();

        if (content == null) {
            return Map.of();
        }

        if (content instanceof Map<?, ?> map) {
            Map<String, Object> normalized =
                    new LinkedHashMap<>();

            map.forEach((key, value) ->
                    normalized.put(
                            String.valueOf(key),
                            value));

            return normalized;
        }

        return Map.of(
                "value",
                content);
    }

    private List<String> textContent(
            CallToolResult result) {

        List<String> values =
                new ArrayList<>();

        if (result.content() == null) {
            return values;
        }

        for (Content content : result.content()) {
            if (content instanceof TextContent textContent) {
                values.add(
                        textContent.text());
            }
        }

        return List.copyOf(values);
    }

    private void validateTrustedContext(
            ToolInvokeContext context) {

        if (context == null) {
            throw new McpClientException(
                    "MCP_TRUSTED_CONTEXT_MISSING",
                    "Tool invocation context is required");
        }

        if (context.getTenantId() == null
                || context.getTenantId() <= 0
                || context.getShopId() == null
                || context.getShopId() <= 0
                || context.getUserId() == null
                || context.getUserId() <= 0
                || context.getTraceId() == null
                || context.getTraceId().isBlank()) {

            throw new McpClientException(
                    "MCP_TRUSTED_CONTEXT_MISSING",
                    "tenantId, shopId, userId and traceId "
                            + "are required for MCP calls");
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new McpClientException(
                    "MCP_SERVER_DISABLED",
                    "MCP server is disabled: "
                            + properties.getServerCode());
        }

        if (properties.getBearerToken() == null
                || properties.getBearerToken().isBlank()) {
            throw new McpClientException(
                    "MCP_CREDENTIAL_MISSING",
                    "MCP bearer token is not configured");
        }
    }

    private McpClientException mapException(
            RuntimeException ex) {

        if (hasCause(
                ex,
                HttpConnectTimeoutException.class)) {

            return new McpClientException(
                    "MCP_CONNECT_TIMEOUT",
                    "MCP connection timed out",
                    ex);
        }

        if (hasCause(ex, ConnectException.class)
                || hasCause(
                        ex,
                        NoRouteToHostException.class)
                || hasCause(
                        ex,
                        UnknownHostException.class)) {

            return new McpClientException(
                    "MCP_CONNECT_FAILED",
                    "Cannot connect to MCP server",
                    ex);
        }

        if (hasCause(ex, HttpTimeoutException.class)
                || hasCause(
                        ex,
                        TimeoutException.class)) {

            return new McpClientException(
                    "MCP_CALL_TIMEOUT",
                    "MCP request timed out",
                    ex);
        }

        String message =
                ex.getMessage() == null
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();

        if (containsMessage(ex, "protocol")
                || containsMessage(ex, "json-rpc")) {

            return new McpClientException(
                    "MCP_PROTOCOL_ERROR",
                    message,
                    ex);
        }

        return new McpClientException(
                "MCP_TRANSPORT_ERROR",
                message,
                ex);
    }

    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> type) {

        Throwable current =
                throwable;

        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }

            if (current.getCause() == current) {
                break;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    private boolean containsMessage(
            Throwable throwable,
            String keyword) {

        Throwable current =
                throwable;

        String normalizedKeyword =
                keyword.toLowerCase();

        while (current != null) {
            String message =
                    current.getMessage();

            if (message != null
                    && message
                            .toLowerCase()
                            .contains(normalizedKeyword)) {
                return true;
            }

            if (current.getCause() == current) {
                break;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    private void protocolEvidence(
            ToolInvokeContext context,
            String method,
            String protocolVersion,
            String remoteToolName) {

        log.info(
                "[MCP-PROTOCOL] method={} "
                        + "serverCode={} "
                        + "protocolVersion={} "
                        + "remoteToolName={} "
                        + "taskId={} "
                        + "stepId={} "
                        + "traceId={} "
                        + "tenantId={} "
                        + "shopId={} "
                        + "userId={} "
                        + "approvalId={}",
                method,
                properties.getServerCode(),
                protocolVersion,
                remoteToolName,
                context.getTaskId(),
                context.getStepId(),
                context.getTraceId(),
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                context.getApprovalId());
    }

    @FunctionalInterface
    private interface ClientOperation<T> {

        T execute(
                McpSyncClient client,
                InitializeResult initializeResult);
    }
}