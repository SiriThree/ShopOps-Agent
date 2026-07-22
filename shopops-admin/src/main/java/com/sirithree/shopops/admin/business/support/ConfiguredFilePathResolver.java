package com.sirithree.shopops.admin.business.support;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfiguredFilePathResolver {
    private ConfiguredFilePathResolver() {
    }

    public static Path resolve(String configuredPath) {
        return resolve(configuredPath, Path.of("").toAbsolutePath().normalize());
    }

    static Path resolve(String configuredPath, Path baseDirectory) {
        Path candidate = Path.of(configuredPath.trim());
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }

        Path current = baseDirectory.toAbsolutePath().normalize();
        while (current != null) {
            Path resolved = current.resolve(candidate).normalize();
            if (Files.exists(resolved)) {
                return resolved;
            }
            current = current.getParent();
        }
        return baseDirectory.toAbsolutePath().normalize().resolve(candidate).normalize();
    }
}
