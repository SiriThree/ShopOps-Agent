package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class BenchmarkManifestValidationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void frozenManifestMatchesHeldOutCaseIdsAndCanonicalHashes() throws Exception {
        Map<String,Object> manifest = readObject("benchmark/v1/benchmark-manifest-v1.json");
        Map<String,Object> benchmarks = (Map<String,Object>) manifest.get("benchmarks");
        int heldOut = 0;
        for (Map.Entry<String,Object> entry : benchmarks.entrySet()) {
            String kind = entry.getKey();
            Map<String,Object> benchmark = (Map<String,Object>) entry.getValue();
            Map<String,Object> splits = (Map<String,Object>) benchmark.get("splits");
            for (Map.Entry<String,Object> splitEntry : splits.entrySet()) {
                Map<String,Object> frozen = (Map<String,Object>) splitEntry.getValue();
                String sourceFile = String.valueOf(frozen.get("sourceFile"));
                String resource = sourceFile.substring(sourceFile.indexOf("src/test/resources/") + "src/test/resources/".length());
                List<Map<String,Object>> all = readList(resource);
                List<Map<String,Object>> selected = all.stream().filter(c -> kind.equals(c.get("benchmarkType"))).toList();
                List<String> ids = selected.stream().map(c -> String.valueOf(c.get("caseId"))).toList();
                assertThat(ids).containsExactlyElementsOf((List<String>) frozen.get("caseIds"));
                assertThat(selected).hasSize(((Number) frozen.get("caseCount")).intValue());
                assertThat(sha256(mapper.writeValueAsString(canonicalize(selected)))).isEqualTo(frozen.get("selectedCasesSha256"));
                if ("test".equals(splitEntry.getKey())) heldOut += selected.size();
            }
        }
        assertThat(heldOut).isEqualTo(((Number)manifest.get("formalHeldOutCaseCount")).intValue());
    }

    private Map<String,Object> readObject(String resource) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return mapper.readValue(in, new TypeReference<>() {});
        }
    }
    private List<Map<String,Object>> readList(String resource) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return mapper.readValue(in, new TypeReference<>() {});
        }
    }
    private Object canonicalize(Object value) {
        if (value instanceof Map<?,?> map) {
            Map<String,Object> sorted = new TreeMap<>();
            map.forEach((k,v) -> sorted.put(String.valueOf(k), canonicalize(v)));
            return sorted;
        }
        if (value instanceof List<?> list) return list.stream().map(this::canonicalize).toList();
        return value;
    }
    private String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
