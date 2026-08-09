package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import java.util.List; import java.util.Map; import org.junit.jupiter.api.Test;
class RecoveryBudgetBoundaryCoverageTest { @Test void causalMatrixContainsLastAllowedAndExhaustedRegions() throws Exception {
 Map<String,Object> doc=Stage4RecoveryDatasetSupport.resource("/benchmark/v1/recovery/stage4/recovery-causal-matrix.json"); @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)doc.get("rows");
 assertThat(rows.stream().map(r->String.valueOf(r.get("budgetRegion")))).contains("LAST_ALLOWED_SUCCESS","BUDGET_EXHAUSTED"); }}
