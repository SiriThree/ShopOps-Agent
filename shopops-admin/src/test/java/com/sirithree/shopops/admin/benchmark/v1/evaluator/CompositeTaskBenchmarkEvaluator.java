package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkMetrics;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import java.util.List;

public class CompositeTaskBenchmarkEvaluator implements BenchmarkEvaluator {
    private final List<BenchmarkEvaluator> evaluators;

    public CompositeTaskBenchmarkEvaluator() {
        this(List.of(
                new BusinessOutcomeEvaluator(),
                new ToolLegalityEvaluator(),
                new GovernanceEvidenceEvaluator(),
                new UnexpectedSideEffectEvaluator(),
                new FinalStateEvaluator()
        ));
    }

    public CompositeTaskBenchmarkEvaluator(List<BenchmarkEvaluator> evaluators) {
        this.evaluators = List.copyOf(evaluators);
    }

    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult aggregate = new EvaluationResult();
        for (BenchmarkEvaluator evaluator : evaluators) {
            aggregate.merge(evaluator.evaluate(benchmarkCase, evidence));
        }

        boolean business = bool(aggregate, "businessOutcomeCorrect");
        boolean tools = bool(aggregate, "toolExecutionValid");
        boolean governance = bool(aggregate, "governanceSatisfied");
        boolean sideEffects = bool(aggregate, "noUnexpectedSideEffect");
        boolean finalState = bool(aggregate, "finalStateCorrect");
        boolean taskSuccess = BenchmarkMetrics.taskSuccess(business, tools, governance, sideEffects, finalState);
        aggregate.metric("taskSuccess", taskSuccess);
        aggregate.passed = taskSuccess && aggregate.failureReasons.isEmpty();
        aggregate.evidenceRefs.addAll(evidence.evidenceRefs);
        return aggregate;
    }

    private boolean bool(EvaluationResult result, String key) {
        return Boolean.TRUE.equals(result.metricValues.get(key));
    }
}
