package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import java.util.Set; import java.util.stream.Collectors; import org.junit.jupiter.api.Test;
class RecoveryExternalRealityCoverageTest { @Test void candidateCoversSucceededAndNotAcceptedTruth() throws Exception {
 Set<String> states=Stage4RecoveryDatasetSupport.recoveryCases().stream().map(v->v.benchmarkCase().initialExternalState).collect(Collectors.toSet()); assertThat(states).contains("SUCCEEDED","NOT_ACCEPTED"); }}
