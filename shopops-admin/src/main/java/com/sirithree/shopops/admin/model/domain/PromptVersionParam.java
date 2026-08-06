package com.sirithree.shopops.admin.model.domain;

import jakarta.validation.constraints.NotBlank;

public class PromptVersionParam {
    @NotBlank
    private String promptName;
    private String taskType;
    @NotBlank
    private String templateContent;
    @NotBlank
    private String version;
    private Boolean active;

    public String getPromptName() { return promptName; }
    public void setPromptName(String promptName) { this.promptName = promptName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
