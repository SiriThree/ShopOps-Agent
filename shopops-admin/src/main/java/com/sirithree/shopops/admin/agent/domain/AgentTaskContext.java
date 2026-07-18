package com.sirithree.shopops.admin.agent.domain;

public class AgentTaskContext {
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private Long taskId;
    private String traceId;
    private AgentTaskCreateParam createParam;

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
}
