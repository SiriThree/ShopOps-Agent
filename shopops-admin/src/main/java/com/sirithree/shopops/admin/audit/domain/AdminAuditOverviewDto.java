package com.sirithree.shopops.admin.audit.domain;

import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminAuditOverviewDto {
    private long authEventTotal;
    private long authFailureTotal;
    private long taskEventTotal;
    private long taskFailureTotal;
    private long toolCallTotal;
    private long toolCallFailed;
    private List<AuthAuditEventDto> recentAuthEvents;
    private List<AgentTaskEventDto> recentTaskEvents;
    private List<Map<String, Object>> recentToolCalls;
    private LocalDateTime generatedAt;

    public long getAuthEventTotal() {
        return authEventTotal;
    }

    public void setAuthEventTotal(long authEventTotal) {
        this.authEventTotal = authEventTotal;
    }

    public long getAuthFailureTotal() {
        return authFailureTotal;
    }

    public void setAuthFailureTotal(long authFailureTotal) {
        this.authFailureTotal = authFailureTotal;
    }

    public long getTaskEventTotal() {
        return taskEventTotal;
    }

    public void setTaskEventTotal(long taskEventTotal) {
        this.taskEventTotal = taskEventTotal;
    }

    public long getTaskFailureTotal() {
        return taskFailureTotal;
    }

    public void setTaskFailureTotal(long taskFailureTotal) {
        this.taskFailureTotal = taskFailureTotal;
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

    public List<AuthAuditEventDto> getRecentAuthEvents() {
        return recentAuthEvents;
    }

    public void setRecentAuthEvents(List<AuthAuditEventDto> recentAuthEvents) {
        this.recentAuthEvents = recentAuthEvents;
    }

    public List<AgentTaskEventDto> getRecentTaskEvents() {
        return recentTaskEvents;
    }

    public void setRecentTaskEvents(List<AgentTaskEventDto> recentTaskEvents) {
        this.recentTaskEvents = recentTaskEvents;
    }

    public List<Map<String, Object>> getRecentToolCalls() {
        return recentToolCalls;
    }

    public void setRecentToolCalls(List<Map<String, Object>> recentToolCalls) {
        this.recentToolCalls = recentToolCalls;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
