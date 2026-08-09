package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import java.util.Set; import java.util.stream.Collectors; import org.junit.jupiter.api.Test;
class IdempotencyPayloadRelationCoverageTest { @Test void candidateCoversSameAndConflictingBusinessPayloadsWithoutInventingUnsupportedCrossKeySemantics() throws Exception {
 Set<String> values=Stage5IdempotencyDatasetSupport.cases().stream().map(v->String.valueOf(v.benchmarkCase().idempotencyExpectation.get("payloadRelation"))).collect(Collectors.toSet());
 assertThat(values).contains("SAME_PAYLOAD","DIFFERENT_BUSINESS_PAYLOAD"); assertThat(values).doesNotContain("DIFFERENT_KEY_SAME_BUSINESS_TARGET"); }}
