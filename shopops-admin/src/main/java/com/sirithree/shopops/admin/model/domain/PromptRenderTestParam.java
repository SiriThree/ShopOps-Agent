package com.sirithree.shopops.admin.model.domain;

import java.util.Map;

public class PromptRenderTestParam {
    private String version;
    private String prompt;
    private Map<String, Object> variables;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
}
