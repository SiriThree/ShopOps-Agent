package com.sirithree.shopops.admin.benchmark.v1.evidence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.domain.TraceSpanDto;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductionBenchmarkEvidenceCollector implements BenchmarkEvidenceCollector {
    private final AgentTaskService agentTaskService;
    private final ToolCallLogService toolCallLogService;
    private final TraceService traceService;
    private final ApprovalRequestService approvalRequestService;
    private final WriteOperationService writeOperationService;
    private final OperationReportService operationReportService;
    private final ObjectMapper objectMapper;
    private final SensitiveEvidenceSanitizer sanitizer;

    public ProductionBenchmarkEvidenceCollector(AgentTaskService agentTaskService,
                                                ToolCallLogService toolCallLogService,
                                                TraceService traceService,
                                                ApprovalRequestService approvalRequestService,
                                                WriteOperationService writeOperationService,
                                                OperationReportService operationReportService,
                                                ObjectMapper objectMapper) {
        this.agentTaskService = agentTaskService;
        this.toolCallLogService = toolCallLogService;
        this.traceService = traceService;
        this.approvalRequestService = approvalRequestService;
        this.writeOperationService = writeOperationService;
        this.operationReportService = operationReportService;
        this.objectMapper = objectMapper;
        this.sanitizer = new SensitiveEvidenceSanitizer();
    }

    @Override
    public CollectedEvidence collect(Long tenantId, Long shopId, Long taskId, String traceId) {
        CollectedEvidence evidence = new CollectedEvidence();
        AgentTaskDto task = agentTaskService.getTask(tenantId, shopId, taskId)
                .orElseThrow(() -> new IllegalStateException("Task disappeared before evidence collection: " + taskId));
        evidence.task = task;
        evidence.steps = agentTaskService.listSteps(tenantId, shopId, taskId);
        evidence.taskEvents = agentTaskService.listEvents(tenantId, shopId, taskId);
        evidence.toolLogs = toolCallLogService.listByTaskId(tenantId, shopId, taskId).stream()
                .map(this::sanitizedMap)
                .toList();
        String effectiveTraceId = traceId == null ? task.getTraceId() : traceId;
        evidence.traceSpans = effectiveTraceId == null ? List.of() : traceService.listSpans(tenantId, effectiveTraceId);

        ApprovalRequestQueryParam approvalQuery = new ApprovalRequestQueryParam();
        approvalQuery.setTaskId(taskId);
        approvalQuery.setPageSize(100);
        evidence.approvals = approvalRequestService.list(tenantId, shopId, approvalQuery).getList();

        evidence.writeOperations = writeOperationService.listByTaskId(tenantId, shopId, taskId);
        if (task.getReportId() != null) {
            evidence.report = operationReportService.getReport(tenantId, shopId, task.getReportId()).orElse(null);
        }

        evidence.faultEvents.addAll(faultEvents(evidence.toolLogs));
        evidence.plannerObservation.putAll(plannerObservation(evidence.traceSpans));
        evidence.businessFacts.putAll(businessFacts(evidence));
        evidence.evidenceRefs.addAll(buildRefs(evidence));
        return evidence;
    }

    private List<Map<String, Object>> faultEvents(List<Map<String, Object>> toolLogs) {
        java.util.ArrayList<Map<String, Object>> events = new java.util.ArrayList<>();
        for (Map<String, Object> log : toolLogs) {
            String errorCode = log.get("errorCode") == null ? null : String.valueOf(log.get("errorCode"));
            if (errorCode == null || !errorCode.contains("INJECTED")) continue;
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("sourceType", "TOOL_CALL_LOG");
            event.put("sourceId", log.get("id"));
            event.put("toolCode", log.get("toolCode"));
            event.put("errorCode", errorCode);
            event.put("createdAt", log.get("createdAt"));
            events.add(event);
        }
        return events;
    }

    private Map<String, Object> plannerObservation(List<TraceSpanDto> spans) {
        for (TraceSpanDto span : spans) {
            if (!"agent.planner".equals(span.getSpanName()) || span.getOutputSummary() == null) continue;
            try {
                Map<String, Object> parsed = objectMapper.readValue(span.getOutputSummary(), new TypeReference<>() {});
                return parsed;
            } catch (Exception ignored) {
                return Map.of("rawSummaryHash", hash(span.getOutputSummary()), "parseStatus", "UNAVAILABLE");
            }
        }
        return Map.of("parseStatus", "UNAVAILABLE");
    }

    private Map<String, Object> businessFacts(CollectedEvidence evidence) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("taskFinalState", evidence.task == null ? null : evidence.task.getStatus());
        facts.put("reportExists", evidence.report != null);
        facts.put("reportStatus", evidence.report == null ? null : evidence.report.getStatus());
        facts.put("writeOperationCount", evidence.writeOperations.size());
        facts.put("approvalCount", evidence.approvals.size());
        facts.put("executedToolCodes", evidence.executedToolCodes());

        if (evidence.report != null && evidence.report.getEvidence() instanceof Map<?, ?> rawEvidence) {
            Object dataSources = rawEvidence.get("dataSources");
            if (dataSources instanceof Map<?, ?> sources) {
                facts.put("reportEvidenceDomains", sources.keySet().stream().map(String::valueOf).toList());
            }
            facts.put("reportIntent", rawEvidence.get("intent"));
            facts.put("reportGenerationMode", rawEvidence.get("generationMode"));
            facts.put("reportToolCodes", rawEvidence.get("toolCodes"));
            facts.put("reportAdDataStatus", rawEvidence.get("adDataStatus"));
        }
        return facts;
    }

    private List<EvidenceRef> buildRefs(CollectedEvidence evidence) {
        java.util.ArrayList<EvidenceRef> refs = new java.util.ArrayList<>();
        if (evidence.task != null) {
            refs.add(ref("AGENT_TASK", evidence.task.getTaskId(), "status=" + evidence.task.getStatus()));
        }
        evidence.steps.forEach(step -> refs.add(ref("AGENT_TASK_STEP", step.getStepId(),
                "step=" + step.getStepNo() + ",tool=" + step.getToolCode() + ",status=" + step.getStatus())));
        evidence.taskEvents.forEach(event -> refs.add(ref("AGENT_TASK_EVENT", event.getEventId(),
                "event=" + event.getEventType() + "," + event.getFromStatus() + "->" + event.getToStatus())));
        evidence.toolLogs.forEach(log -> refs.add(ref("TOOL_CALL_LOG", log.get("id"),
                "tool=" + log.get("toolCode") + ",status=" + log.get("status") + ",error=" + log.get("errorCode"))));
        evidence.traceSpans.forEach(span -> refs.add(ref("TRACE_SPAN", span.getSpanId(),
                "span=" + span.getSpanName() + ",status=" + span.getStatus())));
        evidence.approvals.forEach(approval -> refs.add(ref("APPROVAL", approval.getApprovalId(),
                "tool=" + approval.getToolCode() + ",status=" + approval.getStatus())));
        evidence.writeOperations.forEach(operation -> refs.add(ref("WRITE_OPERATION", operation.getId(),
                "tool=" + operation.getToolCode() + ",status=" + operation.getStatus()
                        + ",request=" + operation.getOperationRequestId())));
        if (evidence.report != null) {
            refs.add(ref("OPERATION_REPORT", evidence.report.getReportId(),
                    "type=" + evidence.report.getReportType() + ",status=" + evidence.report.getStatus()));
        }
        return refs;
    }

    private EvidenceRef ref(String type, Object id, String summary) {
        return new EvidenceRef(type, id == null ? "unavailable" : String.valueOf(id), summary, hash(summary), Instant.now());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizedMap(Map<String, Object> value) {
        Object sanitized = sanitizer.sanitize(value);
        return sanitized instanceof Map<?, ?> map
                ? objectMapper.convertValue(map, new TypeReference<>() {})
                : Map.of();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash evidence summary", ex);
        }
    }
}
