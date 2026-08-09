package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import java.util.List; import java.util.Map; import org.junit.jupiter.api.Test;
class RecoveryCausalRootDistinctnessTest { @Test void acceptedBlueprintsExplainDurableCausalDifference() throws Exception {
 Map<String,Object> doc=Stage4RecoveryDatasetSupport.resource("/benchmark/v1/recovery/stage4/recovery-root-blueprints.json");
 @SuppressWarnings("unchecked") List<Map<String,Object>> accepted=(List<Map<String,Object>>)doc.get("accepted");
 assertThat(accepted).hasSize(8); assertThat(accepted).allSatisfy(b->{ assertThat(String.valueOf(b.get("durableFactDifference"))).isNotBlank(); assertThat(String.valueOf(b.get("recoveryActionDifference"))).isNotBlank(); }); }}
