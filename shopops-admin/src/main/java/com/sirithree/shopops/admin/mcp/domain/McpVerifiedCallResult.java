package com.sirithree.shopops.admin.mcp.domain;

public record McpVerifiedCallResult(
        McpDiscoveredTool discoveredTool,
        McpRemoteCallResult callResult) {
}
