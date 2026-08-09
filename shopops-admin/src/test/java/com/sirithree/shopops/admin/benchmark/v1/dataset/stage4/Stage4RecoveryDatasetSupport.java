package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;

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
import java.util.stream.Collectors;

final class Stage4RecoveryDatasetSupport {
    static final String DEV = "/benchmark/v1/recovery/dev/cases.json";
    static final String VALIDATION = "/benchmark/v1/recovery/validation/cases.json";
    static final String TEST = "/benchmark/v1/recovery/test/cases.json";
    static final List<String> RESOURCES = List.of(DEV, VALIDATION, TEST);

    record CaseView(BenchmarkCase benchmarkCase, String split) {}

    private Stage4RecoveryDatasetSupport() {}

    static List<CaseView> recoveryCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BenchmarkCaseLoader loader = new BenchmarkCaseLoader(mapper);
        List<CaseView> result = new ArrayList<>();
        for (String resource : RESOURCES) {
            String split = resource.contains("/dev/") ? "dev" : resource.contains("/validation/") ? "validation" : "test";
            for (BenchmarkCase benchmarkCase : loader.loadResource(resource)) {
                if (benchmarkCase.benchmarkType == BenchmarkType.RECOVERY) result.add(new CaseView(benchmarkCase, split));
            }
        }
        return result;
    }

    static Map<String, List<CaseView>> roots() throws IOException {
        Map<String, List<CaseView>> roots = new LinkedHashMap<>();
        for (CaseView view : recoveryCases()) {
            roots.computeIfAbsent(view.benchmarkCase().semanticRootId, ignored -> new ArrayList<>()).add(view);
        }
        return roots;
    }

    static long crossSplitRootCount() throws IOException {
        return roots().values().stream().filter(group -> group.stream().map(CaseView::split).distinct().count() > 1).count();
    }

    static long crossSplitParentCount() throws IOException {
        Map<String, String> splitByCase = recoveryCases().stream()
                .collect(Collectors.toMap(view -> view.benchmarkCase().caseId, CaseView::split));
        return recoveryCases().stream().filter(view -> {
            String parent = view.benchmarkCase().parentCaseId;
            return parent != null && splitByCase.containsKey(parent) && !view.split().equals(splitByCase.get(parent));
        }).count();
    }

    static Set<String> testExclusiveRoots() throws IOException {
        return roots().entrySet().stream()
                .filter(entry -> entry.getValue().stream().map(CaseView::split).collect(Collectors.toSet()).equals(Set.of("test")))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    static Map<String, Object> resource(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = Stage4RecoveryDatasetSupport.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource: " + path);
            return mapper.readValue(in, new TypeReference<>() {});
        }
    }
}
