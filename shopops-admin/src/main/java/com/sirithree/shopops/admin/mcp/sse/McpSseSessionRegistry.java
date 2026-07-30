package com.sirithree.shopops.admin.mcp.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class McpSseSessionRegistry {
    private static final long SESSION_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public McpSseSession createSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        SseEmitter emitter = new SseEmitter(SESSION_TIMEOUT_MS);
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(error -> emitters.remove(sessionId));
        return new McpSseSession(sessionId, emitter);
    }

    public boolean send(String sessionId, Map<String, Object> response) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(response, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(sessionId);
            return false;
        }
    }

    public boolean exists(String sessionId) {
        return emitters.containsKey(sessionId);
    }
}
