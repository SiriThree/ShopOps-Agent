package com.sirithree.shopops.admin.benchmark.v1.dataset.stage7a;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test; import java.util.Map;
class TaskScaleupNearDuplicateTest { @Test void everyCandidatePairIsReviewed() throws Exception { Map<String,Object>d=Stage7ATaskDatasetSupport.objectResource("/benchmark/v1/task/stage7a/task-near-duplicate-review.json"); assertThat(d.get("candidateCount")).isEqualTo(d.get("reviewedCount")); assertThat(d.get("unresolvedCount")).isEqualTo(0); }}
