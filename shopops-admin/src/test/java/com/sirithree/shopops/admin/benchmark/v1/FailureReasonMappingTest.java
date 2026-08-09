package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FailureReasonMappingTest {
    @Test
    void mapsPermissionAndArgumentFailuresToStableReasonCodes() {
        assertThat(FailureReasonMapper.fromToolLog(Map.of("errorCode", "TOOL_PERMISSION_DENIED")))
                .isEqualTo(FailureReasonCode.UNAUTHORIZED_EXECUTION);
        assertThat(FailureReasonMapper.fromToolLog(Map.of("errorCode", "TOOL_INPUT_SCHEMA_INVALID")))
                .isEqualTo(FailureReasonCode.INVALID_TOOL_ARGUMENT);
    }
}
