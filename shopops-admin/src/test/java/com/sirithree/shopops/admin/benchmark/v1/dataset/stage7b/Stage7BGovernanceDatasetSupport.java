package com.sirithree.shopops.admin.benchmark.v1.dataset.stage7b;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCaseLoader;
import java.io.*; import java.util.*; import java.util.stream.Collectors;

final class Stage7BGovernanceDatasetSupport {
    static final List<String> RESOURCES=List.of("/benchmark/v1/governance/dev/cases.json","/benchmark/v1/governance/validation/cases.json","/benchmark/v1/governance/test/cases.json");
    record CaseView(BenchmarkCase benchmarkCase,String split) {}
    private Stage7BGovernanceDatasetSupport() {}
    static List<CaseView> cases() throws IOException { ObjectMapper m=new ObjectMapper(); BenchmarkCaseLoader l=new BenchmarkCaseLoader(m); List<CaseView> out=new ArrayList<>(); for(String r:RESOURCES){String s=r.contains("/dev/")?"dev":r.contains("/validation/")?"validation":"test"; for(BenchmarkCase c:l.loadResource(r)) out.add(new CaseView(c,s));} return out; }
    static Map<String,List<CaseView>> roots() throws IOException { Map<String,List<CaseView>> o=new LinkedHashMap<>(); for(CaseView v:cases()) o.computeIfAbsent(v.benchmarkCase().semanticRootId,k->new ArrayList<>()).add(v); return o; }
    static long crossSplitRootCount() throws IOException { return roots().values().stream().filter(g->g.stream().map(CaseView::split).distinct().count()>1).count(); }
    static long crossSplitParentCount() throws IOException { Map<String,String> by=cases().stream().collect(Collectors.toMap(v->v.benchmarkCase().caseId,CaseView::split)); return cases().stream().filter(v->{String p=v.benchmarkCase().parentCaseId; return p!=null&&by.containsKey(p)&&!v.split().equals(by.get(p));}).count(); }
    static Set<String> testRoots() throws IOException { return roots().entrySet().stream().filter(e->e.getValue().stream().map(CaseView::split).collect(Collectors.toSet()).equals(Set.of("test"))).map(Map.Entry::getKey).collect(Collectors.toSet()); }
    static Map<String,Object> objectResource(String path) throws IOException { ObjectMapper m=new ObjectMapper(); try(InputStream in=Stage7BGovernanceDatasetSupport.class.getResourceAsStream(path)){if(in==null)throw new IllegalStateException("Missing "+path); return m.readValue(in,new TypeReference<>(){});} }
}
