package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import java.util.List;

public class OrderReviewOutcomeEvaluator implements CapabilityOutcomeEvaluator {
    private static final List<String> QUERY_TOOLS = List.of(
            "order.query_summary", "comment.query_negative", "product.query_candidates",
            "ad.query_performance", "report.query_external_metrics");

    @Override
    public boolean supports(BenchmarkCase c) {
        return c != null && ("daily_review".equalsIgnoreCase(c.scenario) || "order_review".equalsIgnoreCase(c.scenario));
    }

    @Override
    public EvaluationResult evaluate(BenchmarkCase c, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        boolean report = OutcomeEvaluationSupport.reportRequiredSatisfied(c, evidence, result);
        boolean outputs = OutcomeEvaluationSupport.toolOutputPresent(evidence, "order.query_summary", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "comment.query_negative", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "product.query_candidates", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "ad.query_performance", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "report.query_external_metrics", result);
        boolean dates = OutcomeEvaluationSupport.dateRangeMatchesInput(c, evidence, QUERY_TOOLS, result)
                && OutcomeEvaluationSupport.safeDefaultDateResolved(c, evidence, QUERY_TOOLS, result);
        boolean intent = OutcomeEvaluationSupport.reportIntentMatches(c, evidence, "daily_review", result);
        boolean metrics = OutcomeEvaluationSupport.reportMetricsMatch(evidence, "orderSummary", "order.query_summary",
                        List.of("gmv", "orderCount", "refundAmount", "refundRate", "avgOrderAmount"), result)
                && OutcomeEvaluationSupport.reportMetricsMatch(evidence, "negativeComments", "comment.query_negative",
                        List.of("negativeCount"), result)
                && OutcomeEvaluationSupport.reportMetricsMatch(evidence, "productCandidates", "product.query_candidates",
                        List.of("candidateCount"), result)
                && OutcomeEvaluationSupport.reportMetricsMatch(evidence, "adPerformance", "ad.query_performance",
                        List.of("spend", "impressions", "clicks", "ctr", "roi"), result)
                && OutcomeEvaluationSupport.reportMetricsMatch(evidence, "externalReportMetrics", "report.query_external_metrics",
                        List.of("visitorCount", "conversionRate", "favoriteCount", "cartAddCount"), result);
        boolean claims = OutcomeEvaluationSupport.reportClaimedToolsWereSuccessful(evidence, result);
        boolean correct = report && outputs && dates && intent && metrics && claims;
        OutcomeEvaluationSupport.finish(result, correct, OutcomeEvaluationSupport.details(
                "reportObserved", report, "sourceOutputsObserved", outputs, "dateRangeCorrect", dates,
                "intentCorrect", intent, "reportMetricsMatch", metrics, "reportToolClaimsConsistent", claims));
        return result;
    }
}
