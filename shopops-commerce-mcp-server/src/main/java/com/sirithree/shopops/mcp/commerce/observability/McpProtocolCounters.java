package com.sirithree.shopops.mcp.commerce.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class McpProtocolCounters {
    private final Map<String, AtomicLong> localCounters = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public McpProtocolCounters(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String method) {
        localCounters.computeIfAbsent(method, ignored -> new AtomicLong()).incrementAndGet();
        Counter.builder("shopops.mcp.protocol.requests")
                .description("Observed MCP JSON-RPC methods")
                .tag("method", normalizeTag(method))
                .register(meterRegistry)
                .increment();
    }

    public long count(String method) {
        AtomicLong counter = localCounters.get(method);
        return counter == null ? 0 : counter.get();
    }

    private String normalizeTag(String method) {
        return switch (method) {
            case "initialize" -> "initialize";
            case "tools/list" -> "tools_list";
            case "tools/call" -> "tools_call";
            case "notifications/initialized" -> "initialized_notification";
            default -> "other";
        };
    }
}
