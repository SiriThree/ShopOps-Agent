package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.AdAnalysisOutcomeEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.CapabilityOutcomeEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.CommentHandlingOutcomeEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.OrderReviewOutcomeEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.ProductOptimizationOutcomeEvaluator;
import java.util.List;

/** Dispatches to small deterministic capability judges; it never executes production business behavior. */
public class BusinessOutcomeEvaluator implements BenchmarkEvaluator {
    private final List<CapabilityOutcomeEvaluator> delegates;

    public BusinessOutcomeEvaluator() {
        this(List.of(
                new OrderReviewOutcomeEvaluator(),
                new CommentHandlingOutcomeEvaluator(),
                new ProductOptimizationOutcomeEvaluator(),
                new AdAnalysisOutcomeEvaluator()
        ));
    }

    public BusinessOutcomeEvaluator(List<CapabilityOutcomeEvaluator> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        for (CapabilityOutcomeEvaluator delegate : delegates) {
            if (delegate.supports(benchmarkCase)) return delegate.evaluate(benchmarkCase, evidence);
        }
        EvaluationResult result = new EvaluationResult();
        result.metric("businessOutcomeCorrect", false);
        result.metric("unsupportedScenario", benchmarkCase == null ? null : benchmarkCase.scenario);
        result.fail(FailureReasonCode.REQUIRED_CAPABILITY_MISSING);
        result.fail(FailureReasonCode.BUSINESS_OUTCOME_INCORRECT);
        return result;
    }
}
