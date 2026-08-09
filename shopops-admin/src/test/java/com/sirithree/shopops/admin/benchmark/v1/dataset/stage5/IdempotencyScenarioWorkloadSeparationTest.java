package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import java.util.Map; import org.junit.jupiter.api.Test;
class IdempotencyScenarioWorkloadSeparationTest { @Test void formalOperationsAndAttemptsAreNeverReportedAsBenchmarkCaseCount() throws Exception {
 Map<String,Object> doc=Stage5IdempotencyDatasetSupport.resource("/benchmark/v1/idempotency/stage5/idempotency-workload-profiles.json"); @SuppressWarnings("unchecked") Map<String,Object> formal=(Map<String,Object>)((Map<?,?>)doc.get("profiles")).get("FORMAL");
 assertThat(Stage5IdempotencyDatasetSupport.cases()).hasSize(22); assertThat(Stage5IdempotencyDatasetSupport.roots()).hasSize(16); assertThat(((Number)formal.get("logicalOperationCount")).intValue()).isEqualTo(260); assertThat(((Number)formal.get("plannedRepeatedRequestAttempts")).intValue()).isEqualTo(700); }}
