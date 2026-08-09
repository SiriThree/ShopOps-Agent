package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;

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

final class Stage5IdempotencyDatasetSupport {
    static final String DEV = "/benchmark/v1/idempotency/dev/cases.json";
    static final String VALIDATION = "/benchmark/v1/idempotency/validation/cases.json";
    static final String TEST = "/benchmark/v1/idempotency/test/cases.json";
    static final List<String> RESOURCES = List.of(DEV, VALIDATION, TEST);
    record CaseView(BenchmarkCase benchmarkCase, String split) {}
    private Stage5IdempotencyDatasetSupport() {}

    static List<CaseView> cases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BenchmarkCaseLoader loader = new BenchmarkCaseLoader(mapper);
        List<CaseView> out = new ArrayList<>();
        for (String resource : RESOURCES) {
            String split = resource.contains("/dev/") ? "dev" : resource.contains("/validation/") ? "validation" : "test";
            for (BenchmarkCase c : loader.loadResource(resource)) {
                if (c.benchmarkType == BenchmarkType.IDEMPOTENCY) out.add(new CaseView(c, split));
            }
        }
        return out;
    }

    static Map<String, List<CaseView>> roots() throws IOException {
        Map<String, List<CaseView>> out = new LinkedHashMap<>();
        for (CaseView view : cases()) out.computeIfAbsent(view.benchmarkCase().semanticRootId, ignored -> new ArrayList<>()).add(view);
        return out;
    }

    static long crossSplitRootCount() throws IOException {
        return roots().values().stream().filter(g -> g.stream().map(CaseView::split).distinct().count() > 1).count();
    }

    static long crossSplitParentCount() throws IOException {
        Map<String,String> splitById = cases().stream().collect(Collectors.toMap(v -> v.benchmarkCase().caseId, CaseView::split));
        return cases().stream().filter(v -> {
            String parent = v.benchmarkCase().parentCaseId;
            return parent != null && splitById.containsKey(parent) && !v.split().equals(splitById.get(parent));
        }).count();
    }

    static Set<String> testExclusiveRoots() throws IOException {
        return roots().entrySet().stream()
                .filter(e -> e.getValue().stream().map(CaseView::split).collect(Collectors.toSet()).equals(Set.of("test")))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    static Map<String,Object> resource(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = Stage5IdempotencyDatasetSupport.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource: " + path);
            return mapper.readValue(in, new TypeReference<>() {});
        }
    }
}
