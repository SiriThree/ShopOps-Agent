package com.sirithree.shopops.admin.dashboard.domain;

import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardSummaryDto {
    private AgentTaskMetricsDto taskMetrics;
    private long reportTotal;
    private long toolCallTotal;
    private long toolCallFailed;
    private List<AgentTaskEventDto> recentFailedEvents = List.of();
    private LocalDateTime generatedAt;

    public AgentTaskMetricsDto getTaskMetrics() {
        return taskMetrics;
    }

    public void setTaskMetrics(AgentTaskMetricsDto taskMetrics) {
        this.taskMetrics = taskMetrics;
    }

    public long getReportTotal() {
        return reportTotal;
    }

    public void setReportTotal(long reportTotal) {
        this.reportTotal = reportTotal;
    }

    public long getToolCallTotal() {
        return toolCallTotal;
    }

    public void setToolCallTotal(long toolCallTotal) {
        this.toolCallTotal = toolCallTotal;
    }

    public long getToolCallFailed() {
        return toolCallFailed;
    }

    public void setToolCallFailed(long toolCallFailed) {
        this.toolCallFailed = toolCallFailed;
    }

    public List<AgentTaskEventDto> getRecentFailedEvents() {
        return recentFailedEvents;
    }

    public void setRecentFailedEvents(List<AgentTaskEventDto> recentFailedEvents) {
        this.recentFailedEvents = recentFailedEvents;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
