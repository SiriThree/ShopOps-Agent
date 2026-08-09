package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class OutcomeEvaluationSupport {
    private OutcomeEvaluationSupport() {}

    static boolean reportRequiredSatisfied(BenchmarkCase c, CollectedEvidence evidence, EvaluationResult result) {
        if (!Boolean.TRUE.equals(c.expectedOutcome.get("reportRequired"))) return true;
        if (evidence.report != null) return true;
        result.fail(FailureReasonCode.BUSINESS_DATA_MISSING);
        return false;
    }

    static boolean reportIntentMatches(BenchmarkCase c, CollectedEvidence evidence, String defaultExpected,
                                       EvaluationResult result) {
        String expected = string(c.expectedOutcome.get("expectedIntent"));
        if (expected == null) expected = defaultExpected;
        if (expected == null) return true;
        String actual = string(reportEvidence(evidence).get("intent"));
        boolean matches = expected.equals(actual);
        if (!matches) result.fail(FailureReasonCode.BUSINESS_TARGET_INCORRECT);
        return matches;
    }

    static boolean toolOutputPresent(CollectedEvidence evidence, String toolCode, EvaluationResult result) {
        boolean present = !ToolEvidenceView.successfulOutput(evidence, toolCode).isEmpty();
        if (!present) result.fail(FailureReasonCode.BUSINESS_DATA_MISSING);
        return present;
    }

    static boolean dateRangeMatchesInput(BenchmarkCase c, CollectedEvidence evidence, Collection<String> toolCodes,
                                         EvaluationResult result) {
        Map<String, Object> requested = ToolEvidenceView.map(c.input.get("dateRange"));
        if (requested.isEmpty()) return true;
        String start = string(requested.get("start"));
        String end = string(requested.get("end"));
        boolean correct = true;
        for (String toolCode : toolCodes) {
            Map<String, Object> input = ToolEvidenceView.successfulInput(evidence, toolCode);
            if (input.isEmpty()) continue;
            if (!Objects.equals(start, string(input.get("startDate"))) || !Objects.equals(end, string(input.get("endDate")))) {
                correct = false;
            }
        }
        if (!correct) result.fail(FailureReasonCode.BUSINESS_TARGET_INCORRECT);
        return correct;
    }

    static boolean safeDefaultDateResolved(BenchmarkCase c, CollectedEvidence evidence, Collection<String> toolCodes,
                                           EvaluationResult result) {
        if (!"SAFE_DEFAULT".equalsIgnoreCase(string(c.expectedOutcome.get("parameterResolution")))) return true;
        Map<String, String> observed = new LinkedHashMap<>();
        for (String toolCode : toolCodes) {
            Map<String, Object> input = ToolEvidenceView.successfulInput(evidence, toolCode);
            if (input.isEmpty()) continue;
            String start = string(input.get("startDate"));
            String end = string(input.get("endDate"));
            if (start == null || end == null || start.isBlank() || end.isBlank()) {
                result.fail(FailureReasonCode.BUSINESS_TARGET_INCORRECT);
                return false;
            }
            observed.put(toolCode, start + "|" + end);
        }
        boolean consistent = !observed.isEmpty() && new LinkedHashSet<>(observed.values()).size() == 1;
        if (!consistent) result.fail(FailureReasonCode.BUSINESS_TARGET_INCORRECT);
        return consistent;
    }

    static boolean reportMetricsMatch(CollectedEvidence evidence, String reportSourceKey, String toolCode,
                                      List<String> metricKeys, EvaluationResult result) {
        Map<String, Object> output = ToolEvidenceView.successfulOutput(evidence, toolCode);
        Map<String, Object> source = ToolEvidenceView.map(ToolEvidenceView.map(reportEvidence(evidence).get("dataSources")).get(reportSourceKey));
        Map<String, Object> metrics = ToolEvidenceView.map(source.get("metrics"));
        if (output.isEmpty() || metrics.isEmpty()) {
            result.fail(FailureReasonCode.BUSINESS_DATA_MISSING);
            return false;
        }
        boolean matches = true;
        for (String key : metricKeys) {
            if (output.containsKey(key) && !numericOrObjectEquals(output.get(key), metrics.get(key))) matches = false;
        }
        if (!matches) result.fail(FailureReasonCode.REPORT_INCONSISTENT);
        return matches;
    }

    static boolean reportIdsAreSubsetOfToolOutput(CollectedEvidence evidence, String reportKey, String toolCode,
                                                   String listKey, String idKey, EvaluationResult result) {
        Set<String> reported = strings(reportEvidence(evidence).get(reportKey));
        if (reported.isEmpty()) return true;
        Set<String> observed = new LinkedHashSet<>();
        for (Map<String, Object> item : ToolEvidenceView.listOfMaps(ToolEvidenceView.successfulOutput(evidence, toolCode).get(listKey))) {
            if (item.get(idKey) != null) observed.add(String.valueOf(item.get(idKey)));
        }
        boolean subset = observed.containsAll(reported);
        if (!subset) result.fail(FailureReasonCode.REPORT_INCONSISTENT);
        return subset;
    }

    static boolean reportStringsAreSubsetOfToolOutput(CollectedEvidence evidence, String reportKey, String toolCode,
                                                       String listKey, String field, EvaluationResult result) {
        Set<String> reported = strings(reportEvidence(evidence).get(reportKey));
        if (reported.isEmpty()) return true;
        Set<String> observed = new LinkedHashSet<>();
        for (Map<String, Object> item : ToolEvidenceView.listOfMaps(ToolEvidenceView.successfulOutput(evidence, toolCode).get(listKey))) {
            if (item.get(field) != null) observed.add(String.valueOf(item.get(field)));
        }
        boolean subset = observed.containsAll(reported);
        if (!subset) result.fail(FailureReasonCode.REPORT_INCONSISTENT);
        return subset;
    }

    static boolean reportClaimedToolsWereSuccessful(CollectedEvidence evidence, EvaluationResult result) {
        Set<String> claimed = strings(reportEvidence(evidence).get("toolCodes"));
        Set<String> successful = ToolEvidenceView.successfulTools(evidence);
        boolean consistent = successful.containsAll(claimed);
        if (!consistent) result.fail(FailureReasonCode.REPORT_INCONSISTENT);
        return consistent;
    }

    static Map<String, Object> reportEvidence(CollectedEvidence evidence) {
        if (evidence.report == null) return Map.of();
        return ToolEvidenceView.map(evidence.report.getEvidence());
    }

    static Set<String> strings(Object value) {
        if (value instanceof Collection<?> collection) {
            Set<String> result = new LinkedHashSet<>();
            for (Object item : collection) if (item != null) result.add(String.valueOf(item));
            return result;
        }
        return value == null ? Set.of() : Set.of(String.valueOf(value));
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static boolean numericOrObjectEquals(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return new BigDecimal(String.valueOf(left)).compareTo(new BigDecimal(String.valueOf(right))) == 0;
        }
        return Objects.equals(left, right);
    }

    static void finish(EvaluationResult result, boolean correct, Map<String, Object> details) {
        result.metric("businessOutcomeCorrect", correct);
        result.metric("businessOutcomeDetails", details);
        if (!correct) result.fail(FailureReasonCode.BUSINESS_OUTCOME_INCORRECT);
    }

    static Map<String, Object> details(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
