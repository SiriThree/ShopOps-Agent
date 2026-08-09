package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;

public interface CapabilityOutcomeEvaluator {
    boolean supports(BenchmarkCase benchmarkCase);
    EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence);
}
