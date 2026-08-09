package com.sirithree.shopops.admin.benchmark.v1.evidence;

public interface BenchmarkEvidenceCollector {
    CollectedEvidence collect(Long tenantId, Long shopId, Long taskId, String traceId);
}
