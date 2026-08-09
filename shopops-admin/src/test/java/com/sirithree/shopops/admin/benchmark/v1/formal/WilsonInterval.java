package com.sirithree.shopops.admin.benchmark.v1.formal;

/** Wilson score interval with z=1.959963984540054 for a two-sided 95% interval. */
public final class WilsonInterval {
    private static final double Z = 1.959963984540054;
    private WilsonInterval() {}

    public static Result of(int successes, int total) {
        if (total <= 0 || successes < 0 || successes > total) return new Result(null, null, true);
        double n = total;
        double p = successes / n;
        double z2 = Z * Z;
        double denominator = 1.0 + z2 / n;
        double center = (p + z2 / (2.0 * n)) / denominator;
        double margin = Z * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n) / denominator;
        return new Result(Math.max(0.0, center - margin), Math.min(1.0, center + margin), total < 20);
    }

    public record Result(Double lower95, Double upper95, boolean lowSampleSize) {}
}
