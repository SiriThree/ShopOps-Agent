package com.sirithree.shopops.admin.mcp.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class McpSseSession {
    private final String sessionId;
    private final SseEmitter emitter;

    public McpSseSession(String sessionId, SseEmitter emitter) {
        this.sessionId = sessionId;
        this.emitter = emitter;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }
}
