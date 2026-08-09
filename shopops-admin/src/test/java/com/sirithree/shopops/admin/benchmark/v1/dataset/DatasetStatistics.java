package com.sirithree.shopops.admin.benchmark.v1.dataset;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatasetStatistics {
    public record Summary(int taskCases,
                          int uniqueSemanticTasks,
                          int naturalLanguageVariants,
                          Map<String, Integer> byScenario,
                          Map<String, Integer> byTag,
                          Map<String, Integer> byDifficulty,
                          Map<String, Integer> byOrigin,
                          int humanReviewed) {}

    public Summary summarize(List<BenchmarkCase> cases) {
        int taskCases = 0;
        int humanReviewed = 0;
        Set<String> semantic = new LinkedHashSet<>();
        Set<String> variants = new LinkedHashSet<>();
        Map<String, Integer> scenario = new LinkedHashMap<>();
        Map<String, Integer> tags = new LinkedHashMap<>();
        Map<String, Integer> difficulty = new LinkedHashMap<>();
        Map<String, Integer> origin = new LinkedHashMap<>();
        for (BenchmarkCase c : cases == null ? List.<BenchmarkCase>of() : cases) {
            if (c.benchmarkType != BenchmarkType.TASK) continue;
            taskCases++;
            String semanticKey = c.semanticTaskId == null || c.semanticTaskId.isBlank() ? c.caseId : c.semanticTaskId;
            semantic.add(semanticKey);
            if (c.input != null && c.input.get("userInput") != null) variants.add(String.valueOf(c.input.get("userInput")));
            increment(scenario, c.scenario);
            increment(difficulty, c.difficulty);
            increment(origin, c.origin == null ? "UNSPECIFIED" : c.origin);
            if (c.tags != null) for (String tag : c.tags) increment(tags, tag);
            if (Boolean.TRUE.equals(c.humanReviewed)) humanReviewed++;
        }
        return new Summary(taskCases, semantic.size(), variants.size(), scenario, tags, difficulty, origin, humanReviewed);
    }

    private void increment(Map<String, Integer> target, String key) {
        target.merge(key == null ? "UNAVAILABLE" : key, 1, Integer::sum);
    }
}
