package com.sirithree.shopops.admin.approval.domain;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ApprovalBatchDecisionParam {
    @NotEmpty
    private List<Long> approvalIds;
    private String comment;
    private String confirmText;

    public List<Long> getApprovalIds() { return approvalIds; }
    public void setApprovalIds(List<Long> approvalIds) { this.approvalIds = approvalIds; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getConfirmText() { return confirmText; }
    public void setConfirmText(String confirmText) { this.confirmText = confirmText; }
}
