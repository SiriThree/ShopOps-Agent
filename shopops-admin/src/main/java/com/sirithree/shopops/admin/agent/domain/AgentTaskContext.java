package com.sirithree.shopops.admin.agent.domain;

public class AgentTaskContext {
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private Long taskId;
    private String traceId;
    private AgentTaskCreateParam createParam;
    private java.util.Map<Integer, Long> stepIdByStepNo = new java.util.HashMap<>();
    private String executorSpanId;
    private long startedAtMillis;
    private int repairAttempts;
    private String plannerMode;
    private boolean plannerFallback;
    private String plannerFallbackReason;
    private java.util.List<String> plannedToolCodes = java.util.List.of();

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public AgentTaskCreateParam getCreateParam() {
        return createParam;
    }

    public void setCreateParam(AgentTaskCreateParam createParam) {
        this.createParam = createParam;
    }

    public java.util.Map<Integer, Long> getStepIdByStepNo() {
        return stepIdByStepNo;
    }

    public void setStepIdByStepNo(java.util.Map<Integer, Long> stepIdByStepNo) {
        this.stepIdByStepNo = stepIdByStepNo;
    }

    public Long resolveStepId(Integer stepNo) {
        return stepIdByStepNo.getOrDefault(stepNo, stepNo.longValue());
    }

    public long getStartedAtMillis() { return startedAtMillis; }
    public void setStartedAtMillis(long startedAtMillis) { this.startedAtMillis = startedAtMillis; }
    public int getRepairAttempts() { return repairAttempts; }
    public void setRepairAttempts(int repairAttempts) { this.repairAttempts = repairAttempts; }

    public String getExecutorSpanId() {
        return executorSpanId;
    }

    public void setExecutorSpanId(String executorSpanId) {
        this.executorSpanId = executorSpanId;
    }

    public String getPlannerMode() { return plannerMode; }
    public void setPlannerMode(String plannerMode) { this.plannerMode = plannerMode; }
    public boolean isPlannerFallback() { return plannerFallback; }
    public void setPlannerFallback(boolean plannerFallback) { this.plannerFallback = plannerFallback; }
    public String getPlannerFallbackReason() { return plannerFallbackReason; }
    public void setPlannerFallbackReason(String plannerFallbackReason) { this.plannerFallbackReason = plannerFallbackReason; }
    public java.util.List<String> getPlannedToolCodes() { return plannedToolCodes; }
    public void setPlannedToolCodes(java.util.List<String> plannedToolCodes) {
        this.plannedToolCodes = plannedToolCodes == null ? java.util.List.of() : java.util.List.copyOf(plannedToolCodes);
    }
}
