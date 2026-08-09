package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class IdempotencyReviewStatusTest { @Test void newCasesAreModelReviewedAndNeverClaimHumanReview() throws Exception { assertThat(Stage5IdempotencyDatasetSupport.cases().stream().filter(v->v.benchmarkCase().caseId.startsWith("stage5-"))).hasSize(7).allSatisfy(v->{ assertThat(v.benchmarkCase().reviewStatus).isEqualTo("MODEL_REVIEWED"); assertThat(v.benchmarkCase().humanReviewed).isFalse(); }); }}
