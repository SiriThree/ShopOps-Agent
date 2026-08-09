package com.sirithree.shopops.admin.benchmark.v1.runtime;

import java.util.Locale;

public final class BenchmarkDatasetResources {
    private BenchmarkDatasetResources() {}

    public static String resourceFor(com.sirithree.shopops.admin.benchmark.v1.BenchmarkType benchmarkType, String split) {
        if (benchmarkType == com.sirithree.shopops.admin.benchmark.v1.BenchmarkType.IDEMPOTENCY
                || benchmarkType == com.sirithree.shopops.admin.benchmark.v1.BenchmarkType.RECOVERY
                || benchmarkType == com.sirithree.shopops.admin.benchmark.v1.BenchmarkType.GOVERNANCE) {
            String normalized = split == null ? "dev" : split.trim().toLowerCase(Locale.ROOT);
            if ("smoke".equals(normalized)) normalized = "dev";
            String family = switch (benchmarkType) {
                case IDEMPOTENCY -> "idempotency";
                case RECOVERY -> "recovery";
                case GOVERNANCE -> "governance";
                default -> throw new IllegalArgumentException("Unsupported benchmark family: " + benchmarkType);
            };
            return switch (normalized) {
                case "dev", "validation", "test" -> "/benchmark/v1/" + family + "/" + normalized + "/cases.json";
                default -> throw new IllegalArgumentException("Unsupported " + family + " benchmark split: " + split);
            };
        }
        return resourceFor(split);
    }

    public static String resourceFor(String split) {
        String normalized = split == null ? "smoke" : split.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "smoke" -> "/benchmark/v1/smoke/task-cases.json";
            case "dev" -> "/benchmark/v1/dev/cases.json";
            case "validation" -> "/benchmark/v1/validation/cases.json";
            case "test" -> "/benchmark/v1/test/cases.json";
            default -> throw new IllegalArgumentException("Unsupported benchmark split: " + split);
        };
    }
}
