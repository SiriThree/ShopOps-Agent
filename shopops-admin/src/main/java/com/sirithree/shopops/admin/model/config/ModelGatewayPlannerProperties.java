package com.sirithree.shopops.admin.model.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shopops.model-gateway.planner")
public class ModelGatewayPlannerProperties {
    private boolean enabled;
    private String providerCode = "openai-compatible";
    private String modelName;
    private String promptCode = "daily_review.plan";
    private String promptVersion;
    private Integer timeoutMs;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPromptCode() { return promptCode; }
    public void setPromptCode(String promptCode) { this.promptCode = promptCode; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
}
