package com.sirithree.shopops.admin.mcp.controller;

import com.sirithree.shopops.admin.mcp.service.McpProtocolService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpProtocolController {
    private final McpProtocolService mcpProtocolService;

    public McpProtocolController(McpProtocolService mcpProtocolService) {
        this.mcpProtocolService = mcpProtocolService;
    }

    @PostMapping("/mcp")
    public ResponseEntity<Map<String, Object>> handle(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok()
                .header(McpProtocolService.MCP_PROTOCOL_VERSION_HEADER, McpProtocolService.PROTOCOL_VERSION)
                .body(mcpProtocolService.handle(request));
    }
}
