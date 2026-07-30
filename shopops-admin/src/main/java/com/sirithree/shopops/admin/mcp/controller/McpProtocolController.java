package com.sirithree.shopops.admin.mcp.controller;

import com.sirithree.shopops.admin.mcp.service.McpProtocolService;
import com.sirithree.shopops.admin.mcp.sse.McpSseSession;
import com.sirithree.shopops.admin.mcp.sse.McpSseSessionRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class McpProtocolController {
    private final McpProtocolService mcpProtocolService;
    private final McpSseSessionRegistry sseSessionRegistry;

    public McpProtocolController(McpProtocolService mcpProtocolService,
                                 McpSseSessionRegistry sseSessionRegistry) {
        this.mcpProtocolService = mcpProtocolService;
        this.sseSessionRegistry = sseSessionRegistry;
    }

    @PostMapping("/mcp")
    public ResponseEntity<Map<String, Object>> handle(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok()
                .header(McpProtocolService.MCP_PROTOCOL_VERSION_HEADER, McpProtocolService.PROTOCOL_VERSION)
                .body(mcpProtocolService.handle(request));
    }

    @GetMapping(path = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openSseSession() {
        McpSseSession session = sseSessionRegistry.createSession();
        try {
            session.getEmitter().send(SseEmitter.event()
                    .name("endpoint")
                    .data("/mcp/messages?sessionId=" + session.getSessionId()));
        } catch (Exception ex) {
            session.getEmitter().completeWithError(ex);
        }
        return session.getEmitter();
    }

    @PostMapping("/mcp/messages")
    public ResponseEntity<Map<String, Object>> handleSseMessage(@RequestParam String sessionId,
                                                                @RequestBody Map<String, Object> request) {
        Map<String, Object> response = mcpProtocolService.handle(request);
        if (!sseSessionRegistry.send(sessionId, response)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("accepted", false, "message", "MCP SSE session not found"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accepted", true);
        body.put("sessionId", sessionId);
        body.put("id", request.get("id"));
        return ResponseEntity.accepted().body(body);
    }
}
