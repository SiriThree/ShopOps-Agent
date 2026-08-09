package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import java.util.Set; import java.util.stream.Collectors; import org.junit.jupiter.api.Test;
class RecoveryConcurrencyCoverageTest { @Test void concurrencyIsCountedByRootNotWorkerCount() throws Exception {
 Set<String> roots=Stage4RecoveryDatasetSupport.recoveryCases().stream().filter(v->Boolean.TRUE.equals(v.benchmarkCase().concurrency.get("simultaneous"))).map(v->v.benchmarkCase().semanticRootId).collect(Collectors.toSet()); assertThat(roots).hasSize(3); }}
