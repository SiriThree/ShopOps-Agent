package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentHandlingOutcomeEvaluator implements CapabilityOutcomeEvaluator {
    private static final List<String> QUERY_TOOLS = List.of("order.query_summary", "comment.query_negative", "product.query_candidates");

    @Override
    public boolean supports(BenchmarkCase c) {
        return c != null && ("comment_risk".equalsIgnoreCase(c.scenario) || "comment_handling".equalsIgnoreCase(c.scenario));
    }

    @Override
    public EvaluationResult evaluate(BenchmarkCase c, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        boolean report = OutcomeEvaluationSupport.reportRequiredSatisfied(c, evidence, result);
        boolean outputs = OutcomeEvaluationSupport.toolOutputPresent(evidence, "comment.query_negative", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "product.query_candidates", result)
                && OutcomeEvaluationSupport.toolOutputPresent(evidence, "order.query_summary", result);
        boolean dates = OutcomeEvaluationSupport.dateRangeMatchesInput(c, evidence, QUERY_TOOLS, result)
                && OutcomeEvaluationSupport.safeDefaultDateResolved(c, evidence, QUERY_TOOLS, result);
        boolean intent = OutcomeEvaluationSupport.reportIntentMatches(c, evidence, "comment_risk", result);
        boolean metrics = OutcomeEvaluationSupport.reportMetricsMatch(evidence, "negativeComments", "comment.query_negative",
                        List.of("negativeCount"), result)
                && OutcomeEvaluationSupport.reportMetricsMatch(evidence, "productCandidates", "product.query_candidates",
                        List.of("candidateCount"), result);
        boolean ids = OutcomeEvaluationSupport.reportIdsAreSubsetOfToolOutput(evidence, "riskCommentIds", "comment.query_negative",
                        "riskComments", "commentId", result)
                && OutcomeEvaluationSupport.reportIdsAreSubsetOfToolOutput(evidence, "productIds", "product.query_candidates",
                        "products", "productId", result);
        boolean targetRelation = affectedProductRelation(evidence, result);
        boolean expectedClass = expectedResultClass(c, evidence, result);
        boolean claims = OutcomeEvaluationSupport.reportClaimedToolsWereSuccessful(evidence, result);
        boolean correct = report && outputs && dates && intent && metrics && ids && targetRelation && expectedClass && claims;
        OutcomeEvaluationSupport.finish(result, correct, OutcomeEvaluationSupport.details(
                "reportObserved", report, "sourceOutputsObserved", outputs, "dateRangeCorrect", dates,
                "intentCorrect", intent, "sourceMetricsMatch", metrics, "reportIdsValid", ids,
                "affectedProductRelationValid", targetRelation, "resultClassCorrect", expectedClass));
        return result;
    }

    private boolean affectedProductRelation(CollectedEvidence evidence, EvaluationResult result) {
        Set<String> commentProducts = new LinkedHashSet<>();
        for (Map<String, Object> item : ToolEvidenceView.listOfMaps(
                ToolEvidenceView.successfulOutput(evidence, "comment.query_negative").get("riskComments"))) {
            if (item.get("productId") != null) commentProducts.add(String.valueOf(item.get("productId")));
        }
        Set<String> candidates = new LinkedHashSet<>();
        for (Map<String, Object> item : ToolEvidenceView.listOfMaps(
                ToolEvidenceView.successfulOutput(evidence, "product.query_candidates").get("products"))) {
            if (item.get("productId") != null) candidates.add(String.valueOf(item.get("productId")));
        }
        if (commentProducts.isEmpty()) return true;
        boolean related = commentProducts.stream().anyMatch(candidates::contains);
        if (!related) result.fail(com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode.BUSINESS_TARGET_INCORRECT);
        return related;
    }

    private boolean expectedResultClass(BenchmarkCase c, CollectedEvidence evidence, EvaluationResult result) {
        String expected = OutcomeEvaluationSupport.string(c.expectedOutcome.get("resultClass"));
        if (expected == null) return true;
        Object countValue = ToolEvidenceView.successfulOutput(evidence, "comment.query_negative").get("negativeCount");
        int count = countValue instanceof Number number ? number.intValue() : -1;
        String actual = count == 0 ? "EMPTY" : count > 0 ? "RISK_FOUND" : "UNKNOWN";
        boolean matches = expected.equalsIgnoreCase(actual);
        if (!matches) result.fail(com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode.BUSINESS_RESULT_MISMATCH);
        return matches;
    }
}
