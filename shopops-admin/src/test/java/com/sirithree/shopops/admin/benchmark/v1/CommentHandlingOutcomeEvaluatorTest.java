package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.CommentHandlingOutcomeEvaluator;
import org.junit.jupiter.api.Test;

class CommentHandlingOutcomeEvaluatorTest {
    @Test void relatedCommentAndProductTargetsPass() {
        var result = new CommentHandlingOutcomeEvaluator().evaluate(TaskEvaluationFixtures.benchmarkCase("comment_risk"), TaskEvaluationFixtures.evidence("comment_risk"));
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(true);
    }
    @Test void fabricatedReportProductFailsAgainstSourceOfTruth() {
        var evidence = TaskEvaluationFixtures.evidence("comment_risk");
        @SuppressWarnings("unchecked") var ev = (java.util.Map<String,Object>) evidence.report.getEvidence();
        ev.put("productIds", java.util.List.of(999999));
        var result = new CommentHandlingOutcomeEvaluator().evaluate(TaskEvaluationFixtures.benchmarkCase("comment_risk"), evidence);
        assertThat(result.failureReasons).contains(FailureReasonCode.REPORT_INCONSISTENT);
    }
}
