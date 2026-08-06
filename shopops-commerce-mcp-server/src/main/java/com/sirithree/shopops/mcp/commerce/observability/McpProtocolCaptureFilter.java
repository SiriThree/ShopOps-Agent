package com.sirithree.shopops.mcp.commerce.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/** Captures protocol method names only; arguments, credentials and response bodies are never logged. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class McpProtocolCaptureFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(McpProtocolCaptureFilter.class);

    private final ObjectMapper objectMapper;
    private final McpProtocolCounters counters;

    public McpProtocolCaptureFilter(ObjectMapper objectMapper, McpProtocolCounters counters) {
        this.objectMapper = objectMapper;
        this.counters = counters;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 64 * 1024);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            capture(wrapped, response.getStatus());
        }
    }

    private void capture(ContentCachingRequestWrapper request, int status) {
        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            String method = root.path("method").asText("");
            if (!method.isBlank()) {
                counters.record(method);
                log.info("[MCP-PROTOCOL] method={} httpStatus={}", method, status);
            }
        } catch (RuntimeException | IOException ex) {
            log.debug("Unable to capture MCP method name: {}", ex.getMessage());
        }
    }
}
