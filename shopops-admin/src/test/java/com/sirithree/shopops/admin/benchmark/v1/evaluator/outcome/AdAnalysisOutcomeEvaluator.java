package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdAnalysisOutcomeEvaluator implements CapabilityOutcomeEvaluator {
    private static final List<String> QUERY_TOOLS = List.of("order.query_summary", "ad.query_performance", "report.query_external_metrics");

    @Override
    public boolean supports(BenchmarkCase c) {
        return c != null && ("ad_anomaly".equalsIgnoreCase(c.scenario) || "ad_analysis".equalsIgnoreCase(c.scenario));
    }

    @Override
    public EvaluationResult evaluate(BenchmarkCase c, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        boolean report = OutcomeEvaluationSupport.reportRequiredSatisfied(c, evidence, result);
        boolean outputs = OutcomeEvaluationSupport.toolOutputPresent(evidence, "ad.query_performance", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "report.query_external_metrics", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "order.query_summary", result);
        boolean dates = OutcomeEvaluationSupport.dateRangeMatchesInput(c, evidence, QUERY_TOOLS, result)
                && OutcomeEvaluationSupport.safeDefaultDateResolved(c, evidence, QUERY_TOOLS, result);
        boolean intent = OutcomeEvaluationSupport.reportIntentMatches(c, evidence, "ad_anomaly", result);
        String actualClass = classify(ToolEvidenceView.successfulOutput(evidence, "ad.query_performance"));
        String expectedClass = OutcomeEvaluationSupport.string(c.expectedOutcome.get("resultClass"));
        boolean noData = "NO_DATA".equals(actualClass);
        boolean adMetrics = noData
                ? "NO_DATA".equals(OutcomeEvaluationSupport.string(OutcomeEvaluationSupport.reportEvidence(evidence).get("adDataStatus")))
                : OutcomeEvaluationSupport.reportMetricsMatch(evidence, "adPerformance", "ad.query_performance",
                        List.of("spend", "impressions", "clicks", "ctr", "roi"), result);
        if (!adMetrics && noData) result.fail(FailureReasonCode.REPORT_INCONSISTENT);
        boolean externalMetrics = OutcomeEvaluationSupport.reportMetricsMatch(evidence, "externalReportMetrics", "report.query_external_metrics",
                List.of("visitorCount", "conversionRate", "favoriteCount", "cartAddCount"), result);
        boolean metrics = adMetrics && externalMetrics;
        boolean campaigns = OutcomeEvaluationSupport.reportStringsAreSubsetOfToolOutput(evidence, "campaignNames",
                "ad.query_performance", "campaigns", "campaignName", result);
        boolean classification = expectedClass == null || expectedClass.equalsIgnoreCase(actualClass);
        if (!classification) result.fail(FailureReasonCode.BUSINESS_RESULT_MISMATCH);
        boolean claims = OutcomeEvaluationSupport.reportClaimedToolsWereSuccessful(evidence, result);
        boolean correct = report && outputs && dates && intent && metrics && campaigns && classification && claims;
        OutcomeEvaluationSupport.finish(result, correct, OutcomeEvaluationSupport.details(
                "reportObserved", report, "sourceOutputsObserved", outputs, "dateRangeCorrect", dates,
                "intentCorrect", intent, "sourceMetricsMatch", metrics, "campaignTargetsValid", campaigns,
                "actualResultClass", actualClass, "expectedResultClass", expectedClass));
        return result;
    }

    static String classify(Map<String, Object> output) {
        if (output == null || output.isEmpty()) return "NO_DATA";
        List<Map<String, Object>> campaigns = ToolEvidenceView.listOfMaps(output.get("campaigns"));
        boolean noMetrics = !output.containsKey("spend") && !output.containsKey("roi") && campaigns.isEmpty();
        if (noMetrics) return "NO_DATA";
        if (decimal(output.get("roi")).compareTo(new BigDecimal("3.0")) < 0
                || decimal(output.get("ctr")).compareTo(new BigDecimal("0.03")) < 0
                || campaigns.stream().anyMatch(item -> decimal(item.get("roi")).compareTo(new BigDecimal("3.0")) < 0)) {
            return "RISK_FOUND";
        }
        return "NORMAL";
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ex) { return BigDecimal.ZERO; }
    }
}
