package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import java.util.List;

public class ProductOptimizationOutcomeEvaluator implements CapabilityOutcomeEvaluator {
    private static final List<String> QUERY_TOOLS = List.of("order.query_summary", "product.query_candidates", "comment.query_negative");

    @Override
    public boolean supports(BenchmarkCase c) {
        return c != null && "product_optimization".equalsIgnoreCase(c.scenario);
    }

    @Override
    public EvaluationResult evaluate(BenchmarkCase c, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        boolean report = OutcomeEvaluationSupport.reportRequiredSatisfied(c, evidence, result);
        boolean outputs = OutcomeEvaluationSupport.toolOutputPresent(evidence, "product.query_candidates", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "comment.query_negative", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "order.query_summary", result);
        boolean dates = OutcomeEvaluationSupport.dateRangeMatchesInput(c, evidence, QUERY_TOOLS, result)
                && OutcomeEvaluationSupport.safeDefaultDateResolved(c, evidence, QUERY_TOOLS, result);
        boolean intent = OutcomeEvaluationSupport.reportIntentMatches(c, evidence, "product_optimization", result);
        boolean metrics = OutcomeEvaluationSupport.reportMetricsMatch(evidence, "productCandidates", "product.query_candidates",
                        List.of("candidateCount"), result)
                && OutcomeEvaluationSupport.reportMetricsMatch(evidence, "negativeComments", "comment.query_negative",
                        List.of("negativeCount"), result);
        boolean ids = OutcomeEvaluationSupport.reportIdsAreSubsetOfToolOutput(evidence, "productIds", "product.query_candidates",
                "products", "productId", result);
        boolean recommendation = evidence.report != null && evidence.report.getMarkdown() != null && !evidence.report.getMarkdown().isBlank();
        if (!recommendation) result.fail(com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode.BUSINESS_DATA_MISSING);
        boolean claims = OutcomeEvaluationSupport.reportClaimedToolsWereSuccessful(evidence, result);
        boolean correct = report && outputs && dates && intent && metrics && ids && recommendation && claims;
        OutcomeEvaluationSupport.finish(result, correct, OutcomeEvaluationSupport.details(
                "reportObserved", report, "sourceOutputsObserved", outputs, "dateRangeCorrect", dates,
                "intentCorrect", intent, "sourceMetricsMatch", metrics, "productTargetsValid", ids,
                "recommendationObserved", recommendation));
        return result;
    }
}
