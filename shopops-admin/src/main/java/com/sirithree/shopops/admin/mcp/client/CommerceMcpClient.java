package com.sirithree.shopops.admin.mcp.client;

import com.sirithree.shopops.admin.mcp.domain.McpDiscoveryResult;
import com.sirithree.shopops.admin.mcp.domain.McpRemoteCallResult;
import com.sirithree.shopops.admin.mcp.domain.McpVerifiedCallResult;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.Map;

public interface CommerceMcpClient {
    McpDiscoveryResult discover(ToolInvokeContext context);

    McpRemoteCallResult call(
            ToolInvokeContext context,
            String remoteToolName,
            Map<String, Object> arguments);

    McpVerifiedCallResult discoverAndCall(
            ToolInvokeContext context,
            String remoteToolName,
            Map<String, Object> arguments,
            String expectedSchemaHash);
}
