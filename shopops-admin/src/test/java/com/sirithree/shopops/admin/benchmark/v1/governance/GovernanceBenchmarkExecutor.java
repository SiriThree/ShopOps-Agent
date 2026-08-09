package com.sirithree.shopops.admin.benchmark.v1.governance;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;

public interface GovernanceBenchmarkExecutor {
    EvaluationRecord execute(BenchmarkCase benchmarkCase, BenchmarkRunRequest request, EvaluationRunMetadata metadata);
}
