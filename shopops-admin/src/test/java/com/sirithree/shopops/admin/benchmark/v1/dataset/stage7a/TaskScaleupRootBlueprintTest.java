package com.sirithree.shopops.admin.benchmark.v1.dataset.stage7a;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test; import java.util.Map;
class TaskScaleupRootBlueprintTest { @Test void blueprintKeepsRejectedCandidatesAndAcceptedCounts() throws Exception { Map<String,Object>d=Stage7ATaskDatasetSupport.objectResource("/benchmark/v1/task/stage7a/task-scaleup-root-blueprints.json"); assertThat(d.get("proposedRootCount")).isEqualTo(100); assertThat(d.get("acceptedRootCount")).isEqualTo(64); assertThat(d.get("rejectedRootCount")).isEqualTo(36); }}
