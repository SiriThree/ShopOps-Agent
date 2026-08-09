package com.sirithree.shopops.admin.benchmark.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BenchmarkCaseLoader {
    private final ObjectMapper objectMapper;
    private final BenchmarkCaseValidator validator;

    public BenchmarkCaseLoader(ObjectMapper objectMapper) {
        this(objectMapper, new BenchmarkCaseValidator());
    }

    BenchmarkCaseLoader(ObjectMapper objectMapper, BenchmarkCaseValidator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public List<BenchmarkCase> loadResource(String resource) throws IOException {
        try (InputStream in = BenchmarkCaseLoader.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("Missing benchmark resource: " + resource);
            List<BenchmarkCase> cases = objectMapper.readValue(in, new TypeReference<>() {});
            requireValidAndUnique(cases);
            return cases;
        }
    }

    public List<BenchmarkCase> loadResources(List<String> resources) throws IOException {
        List<BenchmarkCase> all = new ArrayList<>();
        for (String resource : resources) all.addAll(loadResource(resource));
        requireValidAndUnique(all);
        return all;
    }

    public void requireValidAndUnique(List<BenchmarkCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("Benchmark dataset must not be empty");
        Set<String> caseIds = new HashSet<>();
        for (BenchmarkCase c : cases) {
            validator.requireValid(c);
            if (!caseIds.add(c.caseId)) throw new IllegalArgumentException("Duplicate benchmark caseId: " + c.caseId);
        }
    }
}
