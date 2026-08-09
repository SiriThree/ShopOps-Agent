package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import java.util.List; import java.util.Map; import org.junit.jupiter.api.Test;
class IdempotencyRootSplitPlanTest { @Test void sixHistoricalLeaksAreExplicitlyReassignedAndNoRootRemainsCrossSplit() throws Exception {
 Map<String,Object> doc=Stage5IdempotencyDatasetSupport.resource("/benchmark/v1/idempotency/stage5/idempotency-root-split-plan.json");
 @SuppressWarnings("unchecked") List<Map<String,Object>> roots=(List<Map<String,Object>>)doc.get("roots");
 assertThat(roots.stream().filter(r->Boolean.TRUE.equals(r.get("previouslyLeaked")))).hasSize(6); assertThat(Stage5IdempotencyDatasetSupport.crossSplitRootCount()).isZero(); }}
