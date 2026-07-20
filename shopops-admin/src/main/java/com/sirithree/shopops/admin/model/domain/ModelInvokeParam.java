package com.sirithree.shopops.admin.model.domain;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class ModelInvokeParam {
    private String providerCode;
    private String modelName;
    @NotBlank
    private String prompt;
    private String promptCode;
    private String promptVersion;
    private String traceId;
    private Long taskId;
    private Long reportId;
    private Integer timeoutMs;
    private Map<String, Object> metadata;

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getPromptCode() { return promptCode; }
    public void setPromptCode(String promptCode) { this.promptCode = promptCode; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
