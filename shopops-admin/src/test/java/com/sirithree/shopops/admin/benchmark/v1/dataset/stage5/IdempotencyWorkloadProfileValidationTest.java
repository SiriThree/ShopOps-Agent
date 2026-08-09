package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import java.util.Map; import org.junit.jupiter.api.Test;
class IdempotencyWorkloadProfileValidationTest { @Test void smokeIntegrationAndFormalProfilesSeparateScaleFromSemantics() throws Exception {
 Map<String,Object> doc=Stage5IdempotencyDatasetSupport.resource("/benchmark/v1/idempotency/stage5/idempotency-workload-profiles.json"); @SuppressWarnings("unchecked") Map<String,Object> profiles=(Map<String,Object>)doc.get("profiles");
 assertThat(profiles.keySet()).containsExactlyInAnyOrder("SMOKE","INTEGRATION","FORMAL"); @SuppressWarnings("unchecked") Map<String,Object> formal=(Map<String,Object>)profiles.get("FORMAL");
 assertThat(((Number)formal.get("logicalOperationCount")).intValue()).isGreaterThanOrEqualTo(200); assertThat(((Number)formal.get("heldOutMetricLogicalOperations")).intValue()).isEqualTo(240); assertThat(((Number)formal.get("plannedRepeatedRequestAttempts")).intValue()).isEqualTo(700); }}
