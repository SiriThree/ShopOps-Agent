package com.sirithree.shopops.admin.agent.domain;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentExecutionResult {
    private Boolean success;
    private Boolean degraded;
    private Map<String, ToolInvokeResult> stepResults = new LinkedHashMap<>();
    private Long reportId;
    private String errorMessage;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getDegraded() {
        return degraded;
    }

    public void setDegraded(Boolean degraded) {
        this.degraded = degraded;
    }

    public Map<String, ToolInvokeResult> getStepResults() {
        return stepResults;
    }

    public void setStepResults(Map<String, ToolInvokeResult> stepResults) {
        this.stepResults = stepResults;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
