package com.sirithree.shopops.admin.mcp.support;

import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.client.CommerceMcpClient;
import com.sirithree.shopops.admin.mcp.domain.McpClientException;
import com.sirithree.shopops.admin.mcp.domain.McpDiscoveredTool;
import com.sirithree.shopops.admin.mcp.domain.McpDiscoveryResult;
import com.sirithree.shopops.admin.mcp.domain.McpRemoteCallResult;
import com.sirithree.shopops.admin.mcp.domain.McpVerifiedCallResult;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-scope MCP infrastructure boundary. It preserves MCP discovery, schema-hash
 * verification and call semantics without requiring an independently started process
 * during the default Maven unit/integration-test lifecycle.
 */
public final class InMemoryCommerceMcpClient implements CommerceMcpClient {
    private static final String PROTOCOL_VERSION = "2025-11-25";
    private final CommentRiskService commentRiskService;
    private final AtomicInteger discoveryCalls = new AtomicInteger();
    private final AtomicInteger toolCalls = new AtomicInteger();

    public InMemoryCommerceMcpClient(CommentRiskService commentRiskService) {
        this.commentRiskService = commentRiskService;
    }

    @Override
    public McpDiscoveryResult discover(ToolInvokeContext context) {
        validateTrustedContext(context);
        discoveryCalls.incrementAndGet();
        McpDiscoveredTool tool = discoveredTool();
        return new McpDiscoveryResult(
                CommerceMcpContracts.SERVER_CODE,
                PROTOCOL_VERSION,
                "shopops-commerce-test-mcp",
                "test",
                List.of(tool));
    }

    @Override
    public McpRemoteCallResult call(ToolInvokeContext context,
                                    String remoteToolName,
                                    Map<String, Object> arguments) {
        validateTrustedContext(context);
        if (!CommerceMcpContracts.COMMENT_QUERY_NEGATIVE.equals(remoteToolName)) {
            throw new McpClientException("MCP_TOOL_NOT_DISCOVERED",
                    "Test MCP tool is not available: " + remoteToolName);
        }
        long requestedShopId = longValue(arguments.get("shopId"), "shopId");
        if (!Long.valueOf(requestedShopId).equals(context.getShopId())) {
            throw new McpClientException("MCP_SCOPE_MISMATCH",
                    "shopId conflicts with trusted ShopOps scope");
        }
        LocalDate startDate = LocalDate.parse(requiredText(arguments, "startDate"));
        LocalDate endDate = LocalDate.parse(requiredText(arguments, "endDate"));
        int maxStar = arguments.get("minStar") instanceof Number number ? number.intValue() : 3;

        Map<String, Object> result = new LinkedHashMap<>(commentRiskService.queryNegativeComments(
                context.getTenantId(), context.getShopId(), startDate, endDate, maxStar));
        result.put("scope", Map.of(
                "tenantId", context.getTenantId(),
                "shopId", context.getShopId(),
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "maxStar", maxStar));
        toolCalls.incrementAndGet();
        return new McpRemoteCallResult(
                CommerceMcpContracts.SERVER_CODE,
                remoteToolName,
                PROTOCOL_VERSION,
                false,
                result,
                List.of(CommerceMcpContracts.canonicalJson(result)));
    }

    @Override
    public McpVerifiedCallResult discoverAndCall(ToolInvokeContext context,
                                                  String remoteToolName,
                                                  Map<String, Object> arguments,
                                                  String expectedSchemaHash) {
        McpDiscoveryResult discovery = discover(context);
        McpDiscoveredTool tool = discovery.tools().stream()
                .filter(candidate -> remoteToolName.equals(candidate.name()))
                .findFirst()
                .orElseThrow(() -> new McpClientException("MCP_TOOL_NOT_DISCOVERED",
                        "Test MCP tool was not discovered: " + remoteToolName));
        if (expectedSchemaHash == null || expectedSchemaHash.isBlank()) {
            throw new McpClientException("MCP_SCHEMA_HASH_MISSING",
                    "Expected MCP schema hash is required");
        }
        if (!expectedSchemaHash.equals(tool.schemaHash())) {
            throw new McpClientException("MCP_TOOL_SCHEMA_MISMATCH",
                    "Remote schema hash changed for tool: " + remoteToolName);
        }
        return new McpVerifiedCallResult(tool, call(context, remoteToolName, arguments));
    }

    public int discoveryCallCount() {
        return discoveryCalls.get();
    }

    public int toolCallCount() {
        return toolCalls.get();
    }

    public void reset() {
        discoveryCalls.set(0);
        toolCalls.set(0);
    }

    private McpDiscoveredTool discoveredTool() {
        return new McpDiscoveredTool(
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                "Test-scope negative comment query with the production MCP contract",
                CommerceMcpContracts.commentQueryNegativeInputSchema(),
                CommerceMcpContracts.commentQueryNegativeSchemaHash(),
                PROTOCOL_VERSION,
                "shopops-commerce-test-mcp",
                "test");
    }

    private void validateTrustedContext(ToolInvokeContext context) {
        if (context == null || context.getTenantId() == null || context.getShopId() == null
                || context.getUserId() == null || context.getTraceId() == null) {
            throw new McpClientException("MCP_TRUSTED_CONTEXT_MISSING",
                    "tenant, shop, user and trace context are required");
        }
    }

    private long longValue(Object value, String field) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            throw new McpClientException("MCP_INPUT_INVALID", field + " is required");
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw new McpClientException("MCP_INPUT_INVALID", field + " must be an integer", ex);
        }
    }

    private String requiredText(Map<String, Object> values, String field) {
        Object value = values.get(field);
        if (value == null || value.toString().isBlank()) {
            throw new McpClientException("MCP_INPUT_INVALID", field + " is required");
        }
        return value.toString();
    }
}
