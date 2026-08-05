package com.sirithree.shopops.admin.approval.domain;

public final class ApprovalStatus {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String WITHDRAWN = "WITHDRAWN";
    public static final String EXPIRED = "EXPIRED";
    public static final String EXECUTING = "EXECUTING";
    public static final String EXECUTED = "EXECUTED";
    public static final String EXECUTION_FAILED = "EXECUTION_FAILED";

    private ApprovalStatus() {
    }
}
