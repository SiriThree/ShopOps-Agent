package com.sirithree.shopops.admin.reliability.fault;

/**
 * Fault-injection contract shared by reliability benchmarks. The production bean is a no-op; tests can provide a
 * primary implementation. Runtime code never branches on benchmark case IDs or benchmark modes.
 */
@FunctionalInterface
public interface ReliabilityFaultController {
    void hit(ReliabilityFaultPoint point, ReliabilityFaultContext context);
}
