package com.sirithree.shopops.admin.agent.domain;

import com.sirithree.shopops.admin.audit.domain.TraceSpanDto;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import java.util.List;
import java.util.Map;

public class AgentTaskDetailDto {
    private AgentTaskDto task;
    private List<AgentTaskStepDto> steps = List.of();
    private List<AgentTaskEventDto> events = List.of();
    private OperationReportDto report;
    private List<TraceSpanDto> spans = List.of();
    private List<Map<String, Object>> toolCalls = List.of();

    public AgentTaskDto getTask() {
        return task;
    }

    public void setTask(AgentTaskDto task) {
        this.task = task;
    }

    public List<AgentTaskStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<AgentTaskStepDto> steps) {
        this.steps = steps;
    }

    public List<AgentTaskEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<AgentTaskEventDto> events) {
        this.events = events;
    }

    public OperationReportDto getReport() {
        return report;
    }

    public void setReport(OperationReportDto report) {
        this.report = report;
    }

    public List<TraceSpanDto> getSpans() {
        return spans;
    }

    public void setSpans(List<TraceSpanDto> spans) {
        this.spans = spans;
    }

    public List<Map<String, Object>> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<Map<String, Object>> toolCalls) {
        this.toolCalls = toolCalls;
    }
}
