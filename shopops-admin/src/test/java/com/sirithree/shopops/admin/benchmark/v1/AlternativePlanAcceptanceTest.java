package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class AlternativePlanAcceptanceTest {
    @Test void equivalentToolOrderingDoesNotBecomeExactTraceGold() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("comment_risk");
        var normal = TaskEvaluationFixtures.evidence("comment_risk");
        var reordered = TaskEvaluationFixtures.evidence("comment_risk");
        Collections.reverse(reordered.toolLogs);
        var evaluator = new CompositeTaskBenchmarkEvaluator();
        assertThat(evaluator.evaluate(c, normal).metricValues.get("taskSuccess")).isEqualTo(true);
        assertThat(evaluator.evaluate(c, reordered).metricValues.get("taskSuccess")).isEqualTo(true);
    }
}
