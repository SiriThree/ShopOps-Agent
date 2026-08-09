package com.sirithree.shopops.admin.benchmark.v1.fault;

import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultContext;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultController;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic test controller; faults are armed by boundary + hit number, never by benchmark case ID. */
public class DeterministicReliabilityFaultController implements ReliabilityFaultController {
    private final Map<ReliabilityFaultPoint, java.util.Set<Integer>> triggerAt = new ConcurrentHashMap<>();
    private final Map<ReliabilityFaultPoint, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> events = java.util.Collections.synchronizedList(new ArrayList<>());

    public void reset() {
        triggerAt.clear();
        hits.clear();
        events.clear();
    }

    public void arm(ReliabilityFaultPoint point, int hitNumber) {
        if (hitNumber <= 0) throw new IllegalArgumentException("hitNumber must be > 0");
        triggerAt.computeIfAbsent(point, ignored -> ConcurrentHashMap.newKeySet()).add(hitNumber);
    }

    @Override
    public void hit(ReliabilityFaultPoint point, ReliabilityFaultContext context) {
        int hit = hits.computeIfAbsent(point, ignored -> new AtomicInteger()).incrementAndGet();
        boolean injected = triggerAt.getOrDefault(point, java.util.Set.of()).contains(hit);
        events.add(Map.of(
                "point", point.name(),
                "hit", hit,
                "injected", injected,
                "logicalOperationId", safe(context.logicalOperationId()),
                "businessTarget", safe(context.businessTarget()),
                "externalReference", safe(context.externalReference()),
                "timestamp", Instant.now().toString()));
        if (injected) {
            throw new InjectedReliabilityFaultException("Injected reliability fault at " + point + " hit=" + hit);
        }
    }

    public List<Map<String, Object>> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
