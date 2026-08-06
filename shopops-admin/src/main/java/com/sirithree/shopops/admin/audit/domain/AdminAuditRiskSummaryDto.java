package com.sirithree.shopops.admin.audit.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminAuditRiskSummaryDto {
    private long total;
    private long elevatedRiskTotal;
    private Map<String, Long> riskBreakdown;
    private List<AdminAuditTimelineEventDto> recentElevatedRiskEvents;
    private LocalDateTime generatedAt;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getElevatedRiskTotal() {
        return elevatedRiskTotal;
    }

    public void setElevatedRiskTotal(long elevatedRiskTotal) {
        this.elevatedRiskTotal = elevatedRiskTotal;
    }

    public Map<String, Long> getRiskBreakdown() {
        return riskBreakdown;
    }

    public void setRiskBreakdown(Map<String, Long> riskBreakdown) {
        this.riskBreakdown = riskBreakdown;
    }

    public List<AdminAuditTimelineEventDto> getRecentElevatedRiskEvents() {
        return recentElevatedRiskEvents;
    }

    public void setRecentElevatedRiskEvents(List<AdminAuditTimelineEventDto> recentElevatedRiskEvents) {
        this.recentElevatedRiskEvents = recentElevatedRiskEvents;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
