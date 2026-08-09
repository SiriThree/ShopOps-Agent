package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCaseLoader;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Stage2TaskDatasetSupport {
    static final String DEV = "/benchmark/v1/dev/cases.json";
    static final String VALIDATION = "/benchmark/v1/validation/cases.json";
    static final String TEST = "/benchmark/v1/test/cases.json";
    static final List<String> TASK_RESOURCES = List.of(DEV, VALIDATION, TEST);

    record CaseView(BenchmarkCase benchmarkCase, String split) {}

    private Stage2TaskDatasetSupport() {}

    static List<CaseView> taskCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BenchmarkCaseLoader loader = new BenchmarkCaseLoader(mapper);
        List<CaseView> result = new ArrayList<>();
        for (String resource : TASK_RESOURCES) {
            String split = resource.contains("/dev/") ? "dev" : resource.contains("/validation/") ? "validation" : "test";
            for (BenchmarkCase benchmarkCase : loader.loadResource(resource)) {
                if (benchmarkCase.benchmarkType == BenchmarkType.TASK) result.add(new CaseView(benchmarkCase, split));
            }
        }
        return result;
    }

    static Map<String, List<CaseView>> roots() throws IOException {
        Map<String, List<CaseView>> roots = new LinkedHashMap<>();
        for (CaseView view : taskCases()) {
            roots.computeIfAbsent(view.benchmarkCase().semanticRootId, ignored -> new ArrayList<>()).add(view);
        }
        return roots;
    }

    static Map<String, Object> resource(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = Stage2TaskDatasetSupport.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource: " + path);
            return mapper.readValue(in, new TypeReference<>() {});
        }
    }

    static long crossSplitRootCount() throws IOException {
        return roots().values().stream().filter(group -> group.stream().map(CaseView::split).distinct().count() > 1).count();
    }

    static long crossSplitParentCount() throws IOException {
        Map<String, String> splitByCase = new LinkedHashMap<>();
        for (CaseView view : taskCases()) splitByCase.put(view.benchmarkCase().caseId, view.split());
        return taskCases().stream().filter(view -> {
            String parent = view.benchmarkCase().parentCaseId;
            return parent != null && splitByCase.containsKey(parent) && !view.split().equals(splitByCase.get(parent));
        }).count();
    }

    static Set<String> heldOutRoots() throws IOException {
        return taskCases().stream().filter(view -> "test".equals(view.split()))
                .map(view -> view.benchmarkCase().semanticRootId)
                .collect(java.util.stream.Collectors.toSet());
    }
}
