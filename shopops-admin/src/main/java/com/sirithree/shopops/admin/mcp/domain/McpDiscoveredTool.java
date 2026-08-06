package com.sirithree.shopops.admin.mcp.domain;

import java.util.Map;

public record McpDiscoveredTool(
        String name,
        String description,
        Map<String, Object> inputSchema,
        String schemaHash,
        String protocolVersion,
        String serverName,
        String serverVersion) {
}
