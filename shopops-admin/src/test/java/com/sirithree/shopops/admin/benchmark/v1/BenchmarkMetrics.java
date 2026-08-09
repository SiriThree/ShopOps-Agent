package com.sirithree.shopops.admin.benchmark.v1;

public final class BenchmarkMetrics {
    private BenchmarkMetrics() {}

    public static boolean taskSuccess(boolean businessOutcomeCorrect,
                                      boolean toolExecutionValid,
                                      boolean governanceSatisfied,
                                      boolean noUnexpectedSideEffect,
                                      boolean finalStateCorrect) {
        return businessOutcomeCorrect && toolExecutionValid && governanceSatisfied
                && noUnexpectedSideEffect && finalStateCorrect;
    }

    public static int duplicateSideEffects(int actualEffectiveSideEffects, int expectedLogicalSideEffects) {
        requireNonNegative(actualEffectiveSideEffects, "actualEffectiveSideEffects");
        requireNonNegative(expectedLogicalSideEffects, "expectedLogicalSideEffects");
        return Math.max(actualEffectiveSideEffects - expectedLogicalSideEffects, 0);
    }

    public static double duplicateSideEffectRate(int duplicateSideEffects, int expectedLogicalSideEffects) {
        requireNonNegative(duplicateSideEffects, "duplicateSideEffects");
        requireNonNegative(expectedLogicalSideEffects, "expectedLogicalSideEffects");
        if (expectedLogicalSideEffects == 0) return duplicateSideEffects == 0 ? 0.0 : 1.0;
        return (double) duplicateSideEffects / expectedLogicalSideEffects;
    }

    public static boolean converged(boolean terminalStateReached,
                                    boolean localStateConsistentWithExternalReality) {
        return terminalStateReached && localStateConsistentWithExternalReality;
    }

    public static double rate(int numerator, int denominator) {
        requireNonNegative(numerator, "numerator");
        requireNonNegative(denominator, "denominator");
        if (numerator > denominator) throw new IllegalArgumentException("numerator cannot exceed denominator");
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must be >= 0");
    }
}
