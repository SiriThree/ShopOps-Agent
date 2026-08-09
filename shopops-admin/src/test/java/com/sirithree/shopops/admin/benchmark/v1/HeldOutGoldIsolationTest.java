package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRuntimeRequest;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class HeldOutGoldIsolationTest {
    @Test void runtimeRequestTypeHasNoGoldBearingFields() {
        Set<String> fields = Arrays.stream(BenchmarkRuntimeRequest.class.getFields()).map(Field::getName).collect(Collectors.toSet());
        assertThat(fields).containsExactlyInAnyOrder("caseId", "scenario", "input", "identity", "initialState");
        assertThat(fields).doesNotContain("expectedOutcome", "requiredCapabilities", "acceptableTools", "forbiddenTools",
                "sideEffectExpectation", "approvalExpectation", "goldVersion");
    }
}
