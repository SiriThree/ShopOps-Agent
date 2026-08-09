package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import java.util.List; import java.util.Map; import org.junit.jupiter.api.Test;
class RecoveryCandidateFeasibilityTest { @Test void allGeneratedRootsPassedFeasibilityAndRejectedRootsRemainRecorded() throws Exception {
 Map<String,Object> doc=Stage4RecoveryDatasetSupport.resource("/benchmark/v1/recovery/stage4/recovery-root-blueprints.json"); @SuppressWarnings("unchecked") List<Map<String,Object>> accepted=(List<Map<String,Object>>)doc.get("accepted"); @SuppressWarnings("unchecked") List<Map<String,Object>> rejected=(List<Map<String,Object>>)doc.get("rejected");
 assertThat(accepted).allSatisfy(b->assertThat(b.get("feasibilityStatus")).isEqualTo("ACCEPTED")); assertThat(rejected).hasSize(9); }}
