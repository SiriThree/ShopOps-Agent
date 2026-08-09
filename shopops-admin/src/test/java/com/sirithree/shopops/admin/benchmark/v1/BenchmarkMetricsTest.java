package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BenchmarkMetricsTest {
    @Test void taskSuccessRequiresAllFivePredicates() {
        assertThat(BenchmarkMetrics.taskSuccess(true, true, true, true, true)).isTrue();
        assertThat(BenchmarkMetrics.taskSuccess(true, true, true, false, true)).isFalse();
    }

    @Test void duplicateSideEffectsUseEffectiveBusinessEffectsNotAttempts() {
        assertThat(BenchmarkMetrics.duplicateSideEffects(3, 1)).isEqualTo(2);
        assertThat(BenchmarkMetrics.duplicateSideEffects(1, 1)).isZero();
        assertThat(BenchmarkMetrics.duplicateSideEffectRate(2, 1)).isEqualTo(2.0);
    }

    @Test void convergenceRequiresTerminalAndExternalConsistency() {
        assertThat(BenchmarkMetrics.converged(true, true)).isTrue();
        assertThat(BenchmarkMetrics.converged(true, false)).isFalse();
        assertThat(BenchmarkMetrics.converged(false, true)).isFalse();
    }

    @Test void governanceRatesNeedBothPositiveAndNegativeDenominators() {
        assertThat(BenchmarkMetrics.rate(8, 10)).isEqualTo(0.8);
        assertThatThrownBy(() -> BenchmarkMetrics.rate(11, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
