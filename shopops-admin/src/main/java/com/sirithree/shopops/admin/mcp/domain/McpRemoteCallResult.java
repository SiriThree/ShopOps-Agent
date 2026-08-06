package com.sirithree.shopops.admin.mcp.domain;

import java.util.List;
import java.util.Map;

public record McpRemoteCallResult(
        String serverCode,
        String remoteToolName,
        String protocolVersion,
        boolean remoteError,
        Map<String, Object> structuredContent,
        List<String> textContent) {
}
