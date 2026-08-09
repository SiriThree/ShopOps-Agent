package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashSet; import java.util.List; import java.util.Map; import org.junit.jupiter.api.Test;
class RecoveryRootSplitPlanTest { @Test void splitPlanAssignsEveryRecoveryRootExactlyOnce() throws Exception {
 Map<String,Object> doc=Stage4RecoveryDatasetSupport.resource("/benchmark/v1/recovery/stage4/recovery-root-split-plan.json");
 @SuppressWarnings("unchecked") List<Map<String,Object>> roots=(List<Map<String,Object>>)doc.get("roots");
 assertThat(roots).hasSize(15); assertThat(new HashSet<>(roots.stream().map(r->String.valueOf(r.get("semanticRootId"))).toList())).hasSize(15);
 assertThat(roots).allSatisfy(r->assertThat(String.valueOf(r.get("assignedSplit"))).isIn("dev","validation","test")); }}
