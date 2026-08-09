package com.sirithree.shopops.admin.benchmark.v1;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdempotencyCaseValidationTest {
    @Test
    void allVersionedIdempotencyCasesMustValidateAndHaveUniqueIds() throws Exception {
        BenchmarkCaseLoader loader = new BenchmarkCaseLoader(new ObjectMapper());
        BenchmarkCaseValidator validator = new BenchmarkCaseValidator();
        List<BenchmarkCase> all = new ArrayList<>();
        for (String split : List.of("dev", "validation", "test")) {
            List<BenchmarkCase> cases = loader.loadResource("/benchmark/v1/idempotency/" + split + "/cases.json");
            assertThat(cases).isNotEmpty();
            for (BenchmarkCase c : cases) assertThat(validator.validate(c)).as(c.caseId).isEmpty();
            all.addAll(cases);
        }
        Set<String> ids = new HashSet<>();
        for (BenchmarkCase c : all) assertThat(ids.add(c.caseId)).as(c.caseId).isTrue();
        assertThat(all).hasSize(14);
    }
}
