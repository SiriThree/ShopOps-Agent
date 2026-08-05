package com.sirithree.shopops.admin.auth.domain;

public final class PermissionCode {
    public static final String DASHBOARD_READ = "dashboard:read";
    public static final String ORDER_READ = "order:read";
    public static final String ORDER_EXPORT = "order:export";
    public static final String PRODUCT_READ = "product:read";
    public static final String PRODUCT_UPDATE = "product:update";
    public static final String REVIEW_READ = "review:read";
    public static final String REPORT_GENERATE = "report:generate";
    public static final String TASK_READ = "task:read";
    public static final String TASK_CANCEL = "task:cancel";
    public static final String APPROVAL_READ = "approval:read";
    public static final String APPROVAL_REVIEW = "approval:review";
    public static final String CONNECTOR_READ = "connector:read";
    public static final String CONNECTOR_MANAGE = "connector:manage";
    public static final String TOOL_READ = "tool:read";
    public static final String TOOL_EXECUTE = "tool:execute";
    public static final String AGENT_EXECUTE = "agent:execute";
    public static final String AUDIT_READ = "audit:read";
    public static final String USER_MANAGE = "user:manage";

    private PermissionCode() {}
}
