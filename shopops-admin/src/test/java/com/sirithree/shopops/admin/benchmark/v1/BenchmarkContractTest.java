package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkContractTest {
    private final BenchmarkCaseLoader loader = new BenchmarkCaseLoader(new ObjectMapper());

    @Test
    void shouldLoadAndValidateAllV1DatasetSplits() throws Exception {
        List<BenchmarkCase> cases = loader.loadResources(List.of(
                "/benchmark/v1/dev/cases.json",
                "/benchmark/v1/validation/cases.json",
                "/benchmark/v1/test/cases.json",
                "/benchmark/v1/smoke/task-cases.json"));
        assertThat(cases).hasSize(28);
        assertThat(cases).extracting(c -> c.benchmarkType)
                .contains(BenchmarkType.TASK, BenchmarkType.IDEMPOTENCY, BenchmarkType.RECOVERY, BenchmarkType.GOVERNANCE);
    }

    @Test
    void shouldRejectDuplicateCaseIdsAcrossResources() {
        assertThatThrownBy(() -> loader.loadResources(List.of(
                "/benchmark/v1/dev/cases.json",
                "/benchmark/v1/dev/cases.json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate benchmark caseId");
    }

    @Test
    void shouldRejectCaseWithoutBusinessExpectation() {
        BenchmarkCase c = new BenchmarkCase();
        c.caseId = "invalid";
        c.benchmarkType = BenchmarkType.TASK;
        c.scenario = "daily_review";
        c.difficulty = "EASY";
        c.goldVersion = "v1";
        c.input.put("userInput", "x");
        c.identity.put("tenantId", 1);
        assertThatThrownBy(() -> new BenchmarkCaseValidator().requireValid(c))
                .hasMessageContaining("expectedOutcome must not be empty");
    }
}
