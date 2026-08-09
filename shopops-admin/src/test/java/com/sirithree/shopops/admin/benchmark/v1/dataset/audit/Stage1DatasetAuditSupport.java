package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCaseLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Stage1DatasetAuditSupport {
    static final List<String> CASE_RESOURCES = List.of(
            "/benchmark/v1/dev/cases.json",
            "/benchmark/v1/validation/cases.json",
            "/benchmark/v1/test/cases.json",
            "/benchmark/v1/smoke/task-cases.json",
            "/benchmark/v1/idempotency/dev/cases.json",
            "/benchmark/v1/idempotency/validation/cases.json",
            "/benchmark/v1/idempotency/test/cases.json",
            "/benchmark/v1/recovery/dev/cases.json",
            "/benchmark/v1/recovery/validation/cases.json",
            "/benchmark/v1/recovery/test/cases.json",
            "/benchmark/v1/governance/dev/cases.json",
            "/benchmark/v1/governance/validation/cases.json",
            "/benchmark/v1/governance/test/cases.json"
    );

    record AuditMetadata(String caseId, String resourceRole, String split, String semanticRootId,
                         String goldSourceType, String reviewStatus, boolean humanReviewEvidencePresent) {}
    record CaseView(BenchmarkCase benchmarkCase, AuditMetadata metadata) {}

    private Stage1DatasetAuditSupport() {}

    static List<CaseView> load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BenchmarkCaseLoader loader = new BenchmarkCaseLoader(mapper);
        List<BenchmarkCase> cases = loader.loadResources(CASE_RESOURCES);
        Map<String, AuditMetadata> metadata = loadMetadata(mapper);
        List<CaseView> result = new ArrayList<>();
        for (BenchmarkCase benchmarkCase : cases) {
            AuditMetadata entry = metadata.get(benchmarkCase.caseId);
            if (entry == null) throw new IllegalStateException("Missing semantic-root metadata: " + benchmarkCase.caseId);
            result.add(new CaseView(benchmarkCase, entry));
        }
        if (!metadata.keySet().equals(cases.stream().map(c -> c.caseId).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalStateException("Semantic-root map does not exactly match benchmark case resources");
        }
        return result;
    }

    static List<CaseView> dedicated() throws IOException {
        return load().stream().filter(view -> "DEDICATED".equals(view.metadata().resourceRole())).toList();
    }

    static Map<String, List<CaseView>> roots(List<CaseView> cases) {
        Map<String, List<CaseView>> result = new LinkedHashMap<>();
        for (CaseView view : cases) result.computeIfAbsent(view.metadata().semanticRootId(), ignored -> new ArrayList<>()).add(view);
        return result;
    }

    static long crossSplitRootCount(List<CaseView> cases) {
        return roots(cases).values().stream().filter(group -> group.stream().map(view -> view.metadata().split()).filter(Set.of("dev", "validation", "test")::contains).distinct().count() > 1).count();
    }

    static long heldOutRootCount(List<CaseView> cases) {
        return roots(cases).values().stream().filter(group -> group.stream().anyMatch(view -> "test".equals(view.metadata().split()))).count();
    }

    static long testExclusiveRootCount(List<CaseView> cases) {
        return roots(cases).values().stream().filter(group -> {
            Set<String> splits = group.stream().map(view -> view.metadata().split()).filter(Set.of("dev", "validation", "test")::contains).collect(java.util.stream.Collectors.toSet());
            return splits.equals(Set.of("test"));
        }).count();
    }

    private static Map<String, AuditMetadata> loadMetadata(ObjectMapper mapper) throws IOException {
        InputStream selected = Stage1DatasetAuditSupport.class.getResourceAsStream("/benchmark/v1/audit/stage3-semantic-root-map.json");
        if (selected == null) selected = Stage1DatasetAuditSupport.class.getResourceAsStream("/benchmark/v1/audit/stage2-semantic-root-map.json");
        if (selected == null) selected = Stage1DatasetAuditSupport.class.getResourceAsStream("/benchmark/v1/audit/stage1-semantic-root-map.json");
        try (InputStream in = selected) {
            if (in == null) throw new IllegalStateException("Missing semantic-root audit map");
            Map<String, Object> doc = mapper.readValue(in, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) doc.get("cases");
            Map<String, AuditMetadata> result = new LinkedHashMap<>();
            for (Map<String, Object> entry : entries) {
                String caseId = String.valueOf(entry.get("caseId"));
                result.put(caseId, new AuditMetadata(
                        caseId,
                        String.valueOf(entry.get("resourceRole")),
                        String.valueOf(entry.get("split")),
                        String.valueOf(entry.get("semanticRootId")),
                        String.valueOf(entry.get("goldSourceType")),
                        String.valueOf(entry.get("reviewStatus")),
                        Boolean.TRUE.equals(entry.get("humanReviewEvidencePresent"))
                ));
            }
            return result;
        }
    }
}
