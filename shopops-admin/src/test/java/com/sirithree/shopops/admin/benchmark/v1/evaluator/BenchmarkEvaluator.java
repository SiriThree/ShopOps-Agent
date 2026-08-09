package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;

public interface BenchmarkEvaluator {
    EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence);
}
