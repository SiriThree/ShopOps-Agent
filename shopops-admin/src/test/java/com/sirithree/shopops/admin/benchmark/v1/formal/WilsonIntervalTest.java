package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class WilsonIntervalTest {
    @Test void reportsLowSampleWarningAndBoundedInterval() {
        WilsonInterval.Result r=WilsonInterval.of(5,5);
        assertThat(r.lowSampleSize()).isTrue();
        assertThat(r.lower95()).isBetween(0.0,1.0);
        assertThat(r.upper95()).isEqualTo(1.0);
    }
}
