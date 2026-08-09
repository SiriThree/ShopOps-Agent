package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class IdempotencyGoldSourceTest { @Test void allCandidateCasesHaveIndependentDomainOrFaultGoldAndExternalTruth() throws Exception { assertThat(Stage5IdempotencyDatasetSupport.cases()).allSatisfy(v->{ var c=v.benchmarkCase(); assertThat(c.goldSourceType).isIn("DOMAIN_INVARIANT","FAULT_CONTRACT_DERIVED"); assertThat(c.externalSystemMode).isIn("NON_IDEMPOTENT_EXTERNAL","IDEMPOTENT_EXTERNAL"); }); }}
