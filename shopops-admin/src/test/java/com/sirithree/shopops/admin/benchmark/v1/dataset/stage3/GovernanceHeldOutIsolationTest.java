package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GovernanceHeldOutIsolationTest {
    @Test
    void heldOutRootIdsDoNotAppearInProductionOrGovernanceEvaluatorSource() throws Exception {
        Set<String> heldOut = Stage3GovernanceDatasetSupport.testExclusiveRoots();
        assertThat(heldOut).hasSize(18);
        Path repo = findRepoRoot();
        var scanRoots = java.util.stream.Stream.of(
                repo.resolve("shopops-admin/src/main"),
                repo.resolve("shopops-common/src/main"),
                repo.resolve("shopops-commerce-mcp-server/src/main"),
                repo.resolve("shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/evaluator"))
                .filter(Files::exists).toList();
        for (Path root : scanRoots) {
            try (var files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    for (String semanticRootId : heldOut) {
                        assertThat(text).as(file + " must not reference held-out root " + semanticRootId)
                                .doesNotContain(semanticRootId);
                    }
                }
            }
        }
    }

    private static Path findRepoRoot() {
        Path current = Path.of(".").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("shopops-admin")) && Files.exists(candidate.resolve("pom.xml"))) return candidate;
        }
        throw new IllegalStateException("Cannot locate ShopOps repository root from " + current);
    }
}
