package com.sirithree.shopops.admin.business.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguredFilePathResolverTest {
    @TempDir
    private Path tempDir;

    @Test
    void shouldResolveRelativePathFromAncestorDirectoryWhenFileExists() throws Exception {
        Path workspaceRoot = tempDir.resolve("workspace");
        Path moduleDir = workspaceRoot.resolve("shopops-admin");
        Path targetFile = workspaceRoot.resolve("docs/demo-data/olist/order-summary-olist.json");
        Files.createDirectories(moduleDir);
        Files.createDirectories(targetFile.getParent());
        Files.writeString(targetFile, "[]");

        Path resolved = ConfiguredFilePathResolver.resolve("docs/demo-data/olist/order-summary-olist.json", moduleDir);

        assertThat(resolved).isEqualTo(targetFile);
    }

    @Test
    void shouldFallbackToBaseDirectoryWhenRelativePathDoesNotExist() {
        Path baseDirectory = tempDir.resolve("shopops-admin");
        Path resolved = ConfiguredFilePathResolver.resolve("docs/demo-data/olist/missing.json", baseDirectory);

        assertThat(resolved).isEqualTo(baseDirectory.resolve("docs/demo-data/olist/missing.json"));
    }
}
