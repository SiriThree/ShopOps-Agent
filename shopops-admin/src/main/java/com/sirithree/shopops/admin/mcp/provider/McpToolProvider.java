package com.sirithree.shopops.admin.mcp.provider;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.mcp.client.CommerceMcpClient;
import com.sirithree.shopops.admin.mcp.domain.McpClientException;
import com.sirithree.shopops.admin.mcp.domain.McpRemoteCallResult;
import com.sirithree.shopops.admin.mcp.domain.McpVerifiedCallResult;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolProvider;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class McpToolProvider implements ToolProvider {
    private final CommerceMcpClient mcpClient;
    private final JacksonJsonSupport jsonSupport;

    public McpToolProvider(CommerceMcpClient mcpClient, JacksonJsonSupport jsonSupport) {
        this.mcpClient = mcpClient;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public boolean supports(McpToolDto tool) {
        return CommerceMcpContracts.PROVIDER_MCP.equalsIgnoreCase(tool.getProviderType());
    }

    @Override
    public ToolInvokeResult invoke(ToolInvokeContext context, McpToolDto tool, Object input) {
        if (!CommerceMcpContracts.DISCOVERY_READY.equalsIgnoreCase(tool.getDiscoveryStatus())) {
            return ToolInvokeResult.failed("MCP_TOOL_NOT_READY",
                    "Remote MCP tool is not approved for execution: " + tool.getDiscoveryStatus(), null);
        }
        if (tool.getMcpServerCode() == null
                || !CommerceMcpContracts.SERVER_CODE.equals(tool.getMcpServerCode())) {
            return ToolInvokeResult.failed("MCP_SERVER_NOT_FOUND",
                    "Unknown MCP server binding: " + tool.getMcpServerCode(), null);
        }
        if (tool.getRemoteToolName() == null || tool.getRemoteToolName().isBlank()) {
            return ToolInvokeResult.failed("MCP_TOOL_NOT_DISCOVERED",
                    "Remote tool name is not configured", null);
        }
        Map<String, Object> arguments = jsonSupport.toMap(jsonSupport.toJson(input));
        try {
            McpVerifiedCallResult verified = mcpClient.discoverAndCall(
                    context,
                    tool.getRemoteToolName(),
                    arguments,
                    tool.getSchemaHash());
            McpRemoteCallResult remote = verified.callResult();
            if (remote.remoteError()) {
                String message = remote.textContent().isEmpty()
                        ? "Remote MCP tool returned a business error"
                        : String.join("; ", remote.textContent());
                return ToolInvokeResult.failed("MCP_REMOTE_ERROR", message, null);
            }
            Map<String, Object> data = new LinkedHashMap<>(remote.structuredContent());
            data.put("_mcp", Map.of(
                    "serverCode", remote.serverCode(),
                    "remoteToolName", remote.remoteToolName(),
                    "protocolVersion", remote.protocolVersion(),
                    "schemaHash", verified.discoveredTool().schemaHash()));
            return ToolInvokeResult.success(data, null);
        } catch (McpClientException ex) {
            return ToolInvokeResult.failed(ex.getErrorCode(), ex.getMessage(), null);
        }
    }
}
