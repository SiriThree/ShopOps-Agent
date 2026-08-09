package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.benchmark.v1.evidence.*;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BenchmarkEvidenceCollectorTest {
    @Test
    void readsProductionFactServicesAndProducesReferencesWithoutInventingSideEffects() {
        AgentTaskService tasks=mock(AgentTaskService.class); ToolCallLogService logs=mock(ToolCallLogService.class);
        TraceService traces=mock(TraceService.class); ApprovalRequestService approvals=mock(ApprovalRequestService.class);
        WriteOperationService writes=mock(WriteOperationService.class); OperationReportService reports=mock(OperationReportService.class);
        AgentTaskDto task=new AgentTaskDto(); task.setTaskId(10L); task.setStatus("SUCCESS"); task.setTraceId("tr_10");
        when(tasks.getTask(1L,1L,10L)).thenReturn(Optional.of(task));
        when(tasks.listSteps(1L,1L,10L)).thenReturn(List.of()); when(tasks.listEvents(1L,1L,10L)).thenReturn(List.of());
        when(logs.listByTaskId(1L,1L,10L)).thenReturn(List.of(Map.of("id",1,"toolCode","order.query_summary","status","SUCCESS","authorization","secret")));
        when(traces.listSpans(1L,"tr_10")).thenReturn(List.of());
        when(approvals.list(eq(1L),eq(1L),any(ApprovalRequestQueryParam.class))).thenReturn(CommonPage.<ApprovalRequestDto>of(List.of(),1,100,0L));
        when(writes.listByTaskId(1L,1L,10L)).thenReturn(List.of());

        CollectedEvidence e = new ProductionBenchmarkEvidenceCollector(tasks,logs,traces,approvals,writes,reports,new ObjectMapper())
                .collect(1L,1L,10L,null);
        assertThat(e.businessFacts).containsEntry("taskFinalState","SUCCESS");
        assertThat(e.executedToolCodes()).containsExactly("order.query_summary");
        assertThat(e.sideEffects).isEmpty(); // unknown external reality stays unknown/empty, not inferred from a tool attempt.
        assertThat(e.evidenceRefs).extracting(EvidenceRef::sourceType).contains("AGENT_TASK","TOOL_CALL_LOG");
        assertThat(e.toolLogs.get(0).get("authorization")).isEqualTo("[REDACTED]");
    }
}
