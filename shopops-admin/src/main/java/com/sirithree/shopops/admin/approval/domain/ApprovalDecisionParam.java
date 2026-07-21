package com.sirithree.shopops.admin.approval.domain;

public class ApprovalDecisionParam {
    private String comment;
    private String confirmText;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getConfirmText() {
        return confirmText;
    }

    public void setConfirmText(String confirmText) {
        this.confirmText = confirmText;
    }
}
