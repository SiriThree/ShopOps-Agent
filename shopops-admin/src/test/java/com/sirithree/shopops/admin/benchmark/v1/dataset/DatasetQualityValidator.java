package com.sirithree.shopops.admin.benchmark.v1.dataset;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Low-complexity duplicate/near-duplicate checks; intentionally no ML dependency. */
public class DatasetQualityValidator {
    public record Issue(String type, String leftCaseId, String rightCaseId, double similarity) {}

    public List<Issue> findIssues(List<BenchmarkCase> cases) {
        List<Issue> issues = new ArrayList<>();
        if (cases == null) return issues;
        for (int i = 0; i < cases.size(); i++) {
            for (int j = i + 1; j < cases.size(); j++) {
                BenchmarkCase left = cases.get(i);
                BenchmarkCase right = cases.get(j);
                String a = inputText(left);
                String b = inputText(right);
                if (a.equals(b) && !a.isBlank()) {
                    issues.add(new Issue("EXACT_DUPLICATE", left.caseId, right.caseId, 1.0));
                    continue;
                }
                String na = normalize(a);
                String nb = normalize(b);
                if (compact(na).equals(compact(nb)) && !compact(na).isBlank()) {
                    issues.add(new Issue("NORMALIZED_DUPLICATE", left.caseId, right.caseId, 1.0));
                    continue;
                }
                double similarity = jaccard(tokens(na), tokens(nb));
                if (sameParent(left, right) && similarity >= 0.90d) {
                    issues.add(new Issue("TRIVIAL_PARENT_VARIANT", left.caseId, right.caseId, similarity));
                }
            }
        }
        return issues;
    }

    public Map<String, Integer> issueCounts(List<BenchmarkCase> cases) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Issue issue : findIssues(cases)) result.merge(issue.type(), 1, Integer::sum);
        return result;
    }

    public String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String compact(String normalized) {
        return normalized == null ? "" : normalized.replace(" ", "");
    }

    private String inputText(BenchmarkCase c) {
        if (c == null || c.input == null || c.input.get("userInput") == null) return "";
        return String.valueOf(c.input.get("userInput")).trim();
    }

    private Set<String> tokens(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Set<String> tokens = new LinkedHashSet<>();
        String spaced = text.replaceAll("([\\p{IsHan}])", "$1 ").trim();
        tokens.addAll(Arrays.asList(spaced.split("\\s+")));
        tokens.removeIf(String::isBlank);
        return tokens;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> union = new LinkedHashSet<>(a);
        union.addAll(b);
        Set<String> intersection = new LinkedHashSet<>(a);
        intersection.retainAll(b);
        return union.isEmpty() ? 0.0 : ((double) intersection.size()) / union.size();
    }

    private boolean sameParent(BenchmarkCase left, BenchmarkCase right) {
        if (left.semanticTaskId != null && left.semanticTaskId.equals(right.semanticTaskId)) return true;
        if (left.parentCaseId != null && left.parentCaseId.equals(right.caseId)) return true;
        if (right.parentCaseId != null && right.parentCaseId.equals(left.caseId)) return true;
        return left.parentCaseId != null && left.parentCaseId.equals(right.parentCaseId);
    }
}
