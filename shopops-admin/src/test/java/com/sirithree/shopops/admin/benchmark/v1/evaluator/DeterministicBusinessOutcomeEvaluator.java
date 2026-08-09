package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeterministicBusinessOutcomeEvaluator implements BenchmarkEvaluator {
    private static final Map<String, String> DOMAIN_KEYS = Map.of(
            "orders", "orderSummary",
            "comments", "negativeComments",
            "products", "productCandidates",
            "ads", "adPerformance",
            "external_metrics", "externalReportMetrics"
    );

    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        boolean correct = true;

        if (Boolean.TRUE.equals(benchmarkCase.expectedOutcome.get("reportRequired")) && evidence.report == null) {
            correct = false;
        }

        List<String> requiredDomains = strings(benchmarkCase.expectedOutcome.get("requiredEvidenceDomains"));
        Set<String> observedDomains = new LinkedHashSet<>(strings(evidence.businessFacts.get("reportEvidenceDomains")));
        for (String domain : requiredDomains) {
            String actualKey = DOMAIN_KEYS.getOrDefault(domain, domain);
            if (!observedDomains.contains(actualKey)) {
                correct = false;
                result.fail(FailureReasonCode.REQUIRED_CAPABILITY_MISSING);
            }
        }

        Object expectedReportStatus = benchmarkCase.expectedOutcome.get("reportStatus");
        if (expectedReportStatus != null && (evidence.report == null
                || !String.valueOf(expectedReportStatus).equalsIgnoreCase(evidence.report.getStatus()))) {
            correct = false;
        }

        result.metric("businessOutcomeCorrect", correct);
        result.metric("businessFacts", evidence.businessFacts);
        if (!correct) result.fail(FailureReasonCode.BUSINESS_OUTCOME_INCORRECT);
        return result;
    }

    private List<String> strings(Object value) {
        if (value instanceof Collection<?> collection) return collection.stream().map(String::valueOf).toList();
        return value == null ? List.of() : List.of(String.valueOf(value));
    }
}
