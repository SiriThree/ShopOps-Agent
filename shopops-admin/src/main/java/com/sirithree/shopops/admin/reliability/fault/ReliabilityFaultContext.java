package com.sirithree.shopops.admin.reliability.fault;

/** Compact non-payload context for a reliability fault boundary. */
public record ReliabilityFaultContext(
        String operationType,
        String logicalOperationId,
        String businessTarget,
        String externalReference) {
}
