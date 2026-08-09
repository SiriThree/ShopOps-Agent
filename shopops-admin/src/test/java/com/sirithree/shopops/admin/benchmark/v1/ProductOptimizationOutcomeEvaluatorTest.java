package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.ProductOptimizationOutcomeEvaluator;
import org.junit.jupiter.api.Test;

class ProductOptimizationOutcomeEvaluatorTest {
    @Test void productRecommendationMustBeGroundedInCandidateIds() {
        var c = TaskEvaluationFixtures.benchmarkCase("product_optimization");
        var result = new ProductOptimizationOutcomeEvaluator().evaluate(c, TaskEvaluationFixtures.evidence("product_optimization"));
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(true);
    }
}
