package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.*;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkDatasetResources;
import java.util.*;
import org.junit.jupiter.api.Test;
class GovernanceCaseValidationTest {
 @Test void allGovernanceSplitsValidateAndContainPositiveAndNegative() throws Exception { BenchmarkCaseLoader loader=new BenchmarkCaseLoader(new ObjectMapper()); BenchmarkCaseValidator validator=new BenchmarkCaseValidator(); Set<String> ids=new HashSet<>(); int neg=0,pos=0; for(String split:List.of("dev","validation","test")){ for(BenchmarkCase c:loader.loadResource(BenchmarkDatasetResources.resourceFor(BenchmarkType.GOVERNANCE,split))){ assertThat(validator.validate(c)).as(c.caseId).isEmpty(); assertThat(ids.add(c.caseId)).isTrue(); if("NEGATIVE".equals(c.governanceCaseClass))neg++; if("POSITIVE".equals(c.governanceCaseClass))pos++; }} assertThat(neg).isGreaterThan(0); assertThat(pos).isGreaterThan(0); }
}
