package com.sirithree.shopops.admin.model.domain;

public class PromptRenderResult {
    private String promptCode;
    private String version;
    private String renderedPrompt;

    public String getPromptCode() { return promptCode; }
    public void setPromptCode(String promptCode) { this.promptCode = promptCode; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getRenderedPrompt() { return renderedPrompt; }
    public void setRenderedPrompt(String renderedPrompt) { this.renderedPrompt = renderedPrompt; }
}
