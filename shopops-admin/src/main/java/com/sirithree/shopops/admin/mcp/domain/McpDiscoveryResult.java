package com.sirithree.shopops.admin.mcp.domain;

import java.util.List;

public record McpDiscoveryResult(
        String serverCode,
        String protocolVersion,
        String serverName,
        String serverVersion,
        List<McpDiscoveredTool> tools) {
}
