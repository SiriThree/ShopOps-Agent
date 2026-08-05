package com.sirithree.shopops.admin.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ShopOpsMetrics {
    private final MeterRegistry registry;

    public ShopOpsMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordTaskCreated() { counter("shopops.task.created").increment(); }
    public void recordTaskCompleted(String outcome) {
        registry.counter("shopops.task.completed", "outcome", safeOutcome(outcome)).increment();
    }
    public void recordToolCall(String toolCode, String outcome, Duration duration) {
        registry.counter("shopops.tool.calls", "tool", boundedTool(toolCode), "outcome", safeOutcome(outcome)).increment();
        Timer.builder("shopops.tool.duration").tag("tool", boundedTool(toolCode)).publishPercentileHistogram().register(registry).record(duration);
    }
    public void recordConnectorCall(String outcome) { registry.counter("shopops.connector.calls", "outcome", safeOutcome(outcome)).increment(); }
    public void recordTenantAccessDenied() { counter("shopops.security.tenant_access_denied").increment(); }
    public void recordIdempotencyHit() { counter("shopops.write.idempotency_hits").increment(); }
    public void recordLeaseExpired() { counter("shopops.task.lease_expired").increment(); }

    private Counter counter(String name) { return registry.counter(name); }
    private String safeOutcome(String value) { return value == null || value.isBlank() ? "unknown" : value.toLowerCase(); }
    private String boundedTool(String value) { return value == null || value.isBlank() ? "unknown" : value.replaceAll("[^a-zA-Z0-9_.-]", "_"); }
}
