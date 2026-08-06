package com.sirithree.shopops.admin.tool.domain;

import java.util.Set;

public class ToolInvokeContext {
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private Long taskId;
    private Long stepId;
    private String traceId;
    private String parentSpanId;
    private Long approvalId;
    private Set<String> permissions = Set.of();
    private Boolean manualInvoke = false;

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

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public Boolean getManualInvoke() {
        return manualInvoke;
    }

    public void setManualInvoke(Boolean manualInvoke) {
        this.manualInvoke = manualInvoke;
    }
}
