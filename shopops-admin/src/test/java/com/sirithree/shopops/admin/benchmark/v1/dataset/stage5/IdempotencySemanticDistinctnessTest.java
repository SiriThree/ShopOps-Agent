package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import java.util.List; import java.util.Map; import org.junit.jupiter.api.Test;
class IdempotencySemanticDistinctnessTest { @Test void authorCriticAcceptedSevenNewCausalScenariosAndRejectedWorkloadOnlyVariants() throws Exception {
 Map<String,Object> doc=Stage5IdempotencyDatasetSupport.resource("/benchmark/v1/idempotency/stage5/idempotency-scenario-blueprints.json");
 assertThat(((Number)doc.get("acceptedCount")).intValue()).isEqualTo(7); assertThat(((Number)doc.get("rejectedCount")).intValue()).isEqualTo(7);
 @SuppressWarnings("unchecked") List<Map<String,Object>> rejected=(List<Map<String,Object>>)doc.get("rejected"); assertThat(rejected).anyMatch(r->"NOT_SEMANTICALLY_DISTINCT".equals(r.get("rejectionTaxonomy"))); }}
