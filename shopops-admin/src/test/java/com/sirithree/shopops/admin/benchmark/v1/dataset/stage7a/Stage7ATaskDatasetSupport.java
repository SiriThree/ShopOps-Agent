package com.sirithree.shopops.admin.benchmark.v1.dataset.stage7a;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCaseLoader;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class Stage7ATaskDatasetSupport {
    static final List<String> RESOURCES=List.of("/benchmark/v1/dev/cases.json","/benchmark/v1/validation/cases.json","/benchmark/v1/test/cases.json");
    record CaseView(BenchmarkCase benchmarkCase,String split) {}
    private Stage7ATaskDatasetSupport() {}
    static List<CaseView> taskCases() throws IOException {
        ObjectMapper mapper=new ObjectMapper(); BenchmarkCaseLoader loader=new BenchmarkCaseLoader(mapper); List<CaseView> out=new ArrayList<>();
        for(String r:RESOURCES){ String split=r.contains("/dev/")?"dev":r.contains("/validation/")?"validation":"test"; for(BenchmarkCase c:loader.loadResource(r)) if(c.benchmarkType== BenchmarkType.TASK) out.add(new CaseView(c,split)); }
        return out;
    }
    static Map<String,List<CaseView>> roots() throws IOException { Map<String,List<CaseView>> out=new LinkedHashMap<>(); for(CaseView v:taskCases()) out.computeIfAbsent(v.benchmarkCase().semanticRootId,k->new ArrayList<>()).add(v); return out; }
    static long crossSplitRootCount() throws IOException { return roots().values().stream().filter(g->g.stream().map(CaseView::split).distinct().count()>1).count(); }
    static long crossSplitParentCount() throws IOException { Map<String,String> splitById=taskCases().stream().collect(Collectors.toMap(v->v.benchmarkCase().caseId,CaseView::split)); return taskCases().stream().filter(v->{String p=v.benchmarkCase().parentCaseId; return p!=null&&splitById.containsKey(p)&&!v.split().equals(splitById.get(p));}).count(); }
    static Set<String> testExclusiveRoots() throws IOException { return roots().entrySet().stream().filter(e->e.getValue().stream().map(CaseView::split).collect(Collectors.toSet()).equals(Set.of("test"))).map(Map.Entry::getKey).collect(Collectors.toSet()); }
    static Map<String,Object> objectResource(String path) throws IOException { ObjectMapper m=new ObjectMapper(); try(InputStream in=Stage7ATaskDatasetSupport.class.getResourceAsStream(path)){ if(in==null) throw new IllegalStateException("Missing resource: "+path); return m.readValue(in,new TypeReference<>(){});} }
    static List<Map<String,Object>> listResource(String path) throws IOException { ObjectMapper m=new ObjectMapper(); try(InputStream in=Stage7ATaskDatasetSupport.class.getResourceAsStream(path)){ if(in==null) throw new IllegalStateException("Missing resource: "+path); return m.readValue(in,new TypeReference<>(){});} }
}
