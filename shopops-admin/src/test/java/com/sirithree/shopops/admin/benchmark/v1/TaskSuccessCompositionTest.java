package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TaskSuccessCompositionTest {
    @Test void everySubconditionIsMandatory() {
        assertThat(BenchmarkMetrics.taskSuccess(true,true,true,true,true)).isTrue();
        assertThat(BenchmarkMetrics.taskSuccess(false,true,true,true,true)).isFalse();
        assertThat(BenchmarkMetrics.taskSuccess(true,false,true,true,true)).isFalse();
        assertThat(BenchmarkMetrics.taskSuccess(true,true,false,true,true)).isFalse();
        assertThat(BenchmarkMetrics.taskSuccess(true,true,true,false,true)).isFalse();
        assertThat(BenchmarkMetrics.taskSuccess(true,true,true,true,false)).isFalse();
    }
}
