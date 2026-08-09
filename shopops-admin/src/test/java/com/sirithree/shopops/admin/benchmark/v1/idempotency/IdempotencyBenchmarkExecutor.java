package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;

/** Phase-specific case driver plugged into the unified ShopOpsBenchmarkRunner. */
@FunctionalInterface
public interface IdempotencyBenchmarkExecutor {
    EvaluationRecord execute(BenchmarkCase benchmarkCase, BenchmarkRunRequest request, EvaluationRunMetadata metadata);
}
