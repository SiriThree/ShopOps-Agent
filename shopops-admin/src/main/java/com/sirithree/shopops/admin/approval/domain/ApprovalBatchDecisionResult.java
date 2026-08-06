package com.sirithree.shopops.admin.approval.domain;

import java.util.ArrayList;
import java.util.List;

public class ApprovalBatchDecisionResult {
    private int requestedCount;
    private int successCount;
    private int failedCount;
    private List<ApprovalRequestDto> succeeded = new ArrayList<>();
    private List<Long> failedApprovalIds = new ArrayList<>();

    public int getRequestedCount() { return requestedCount; }
    public void setRequestedCount(int requestedCount) { this.requestedCount = requestedCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<ApprovalRequestDto> getSucceeded() { return succeeded; }
    public void setSucceeded(List<ApprovalRequestDto> succeeded) { this.succeeded = succeeded; }
    public List<Long> getFailedApprovalIds() { return failedApprovalIds; }
    public void setFailedApprovalIds(List<Long> failedApprovalIds) { this.failedApprovalIds = failedApprovalIds; }
}
