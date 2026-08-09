package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRuntimeRequest;
import org.junit.jupiter.api.Test;

class GoldLeakageProtectionTest {
    @Test
    void runtimeRequestContainsOnlyRuntimeVisibleFields() throws Exception {
        BenchmarkCase c = new BenchmarkCase();
        c.caseId = "leak-test";
        c.scenario = "daily_review";
        c.input.put("userInput", "review today");
        c.identity.put("tenantId", 1);
        c.expectedOutcome.put("secretGold", "NEVER_SEND_THIS");
        c.requiredCapabilities.add("secret-capability");
        c.acceptableTools.add("secret-acceptable-tool");
        c.forbiddenTools.add("secret-forbidden-tool");
        c.goldVersion = "secret-gold-version";

        String json = new ObjectMapper().writeValueAsString(BenchmarkRuntimeRequest.from(c));

        assertThat(json).contains("review today").contains("tenantId");
        assertThat(json).doesNotContain("expectedOutcome", "requiredCapabilities", "acceptableTools",
                "forbiddenTools", "sideEffectExpectation", "approvalExpectation", "goldVersion",
                "NEVER_SEND_THIS", "secret-capability", "secret-forbidden-tool", "secret-gold-version");
    }
}
