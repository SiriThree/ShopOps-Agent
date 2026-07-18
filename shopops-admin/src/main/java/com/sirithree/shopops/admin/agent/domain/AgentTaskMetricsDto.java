package com.sirithree.shopops.admin.agent.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentTaskMetricsDto {
    private long total;
    private long created;
    private long queued;
    private long running;
    private long success;
    private long failed;
    private long degraded;
    private double successRate;
    private long avgLatencyMs;
    private Map<String, Long> statusBreakdown = new LinkedHashMap<>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getQueued() {
        return queued;
    }

    public void setQueued(long queued) {
        this.queued = queued;
    }

    public long getRunning() {
        return running;
    }

    public void setRunning(long running) {
        this.running = running;
    }

    public long getSuccess() {
        return success;
    }

    public void setSuccess(long success) {
        this.success = success;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getDegraded() {
        return degraded;
    }

    public void setDegraded(long degraded) {
        this.degraded = degraded;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(long avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public Map<String, Long> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(Map<String, Long> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }
}
