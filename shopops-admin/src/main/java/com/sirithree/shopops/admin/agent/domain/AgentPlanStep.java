package com.sirithree.shopops.admin.agent.domain;

public class AgentPlanStep {
    private Integer stepNo;
    private String stepName;
    private String toolCode;
    private String reason;

    public AgentPlanStep(Integer stepNo, String stepName, String toolCode) {
        this(stepNo, stepName, toolCode, null);
    }

    public AgentPlanStep(Integer stepNo, String stepName, String toolCode, String reason) {
        this.stepNo = stepNo;
        this.stepName = stepName;
        this.toolCode = toolCode;
        this.reason = reason;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
