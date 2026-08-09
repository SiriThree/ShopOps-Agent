package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.AdAnalysisOutcomeEvaluator;
import org.junit.jupiter.api.Test;

class AdAnalysisOutcomeEvaluatorTest {
    @Test void lowRoiCampaignIsClassifiedFromToolSourceNotReportWording() {
        var result = new AdAnalysisOutcomeEvaluator().evaluate(TaskEvaluationFixtures.benchmarkCase("ad_anomaly"), TaskEvaluationFixtures.evidence("ad_anomaly"));
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(true);
        @SuppressWarnings("unchecked") var details = (java.util.Map<String,Object>) result.metricValues.get("businessOutcomeDetails");
        assertThat(details).containsEntry("actualResultClass", "RISK_FOUND");
    }
}
