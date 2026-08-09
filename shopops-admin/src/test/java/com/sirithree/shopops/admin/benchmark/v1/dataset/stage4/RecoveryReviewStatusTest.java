package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class RecoveryReviewStatusTest { @Test void newCasesAreModelReviewedAndNeverClaimHumanReview() throws Exception { assertThat(Stage4RecoveryDatasetSupport.recoveryCases().stream().filter(v->v.benchmarkCase().caseId.startsWith("stage4-"))).allSatisfy(v->{ assertThat(v.benchmarkCase().reviewStatus).isEqualTo("MODEL_REVIEWED"); assertThat(v.benchmarkCase().humanReviewed).isFalse(); }); }}
