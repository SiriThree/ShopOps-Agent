package com.sirithree.shopops.admin.benchmark.v1.evidence;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.audit.domain.TraceSpanDto;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CollectedEvidence {
    public AgentTaskDto task;
    public List<AgentTaskStepDto> steps = new ArrayList<>();
    public List<AgentTaskEventDto> taskEvents = new ArrayList<>();
    public List<Map<String, Object>> toolLogs = new ArrayList<>();
    public List<TraceSpanDto> traceSpans = new ArrayList<>();
    public List<ApprovalRequestDto> approvals = new ArrayList<>();
    public List<WriteOperation> writeOperations = new ArrayList<>();
    public OperationReportDto report;
    public List<Map<String, Object>> sideEffects = new ArrayList<>();
    public List<Map<String, Object>> faultEvents = new ArrayList<>();
    public List<Map<String, Object>> logicalWriteRequests = new ArrayList<>();
    public List<Map<String, Object>> deliveryAttempts = new ArrayList<>();
    public List<Map<String, Object>> executionAttempts = new ArrayList<>();
    public List<Map<String, Object>> externalAttempts = new ArrayList<>();
    public List<Map<String, Object>> externalEffects = new ArrayList<>();
    public List<Map<String, Object>> idempotencyDecisions = new ArrayList<>();
    public List<Map<String, Object>> idempotencyAttributionAttempts = new ArrayList<>();
    public List<Map<String, Object>> writeOperationTransitions = new ArrayList<>();
    public Map<String, Object> plannerObservation = new LinkedHashMap<>();
    public Map<String, Object> businessFacts = new LinkedHashMap<>();
    public Map<String, Object> governanceDecision = new LinkedHashMap<>();
    public Map<String, Object> authorizationSnapshot = new LinkedHashMap<>();
    public List<EvidenceRef> evidenceRefs = new ArrayList<>();

    public List<String> executedToolCodes() {
        return toolLogs.stream()
                .map(log -> log.get("toolCode"))
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }
}
