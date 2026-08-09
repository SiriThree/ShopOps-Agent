package com.sirithree.shopops.admin.benchmark.v1.recovery;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;

public interface RecoveryBenchmarkExecutor {
    EvaluationRecord execute(BenchmarkCase benchmarkCase, BenchmarkRunRequest request, EvaluationRunMetadata metadata);
}
