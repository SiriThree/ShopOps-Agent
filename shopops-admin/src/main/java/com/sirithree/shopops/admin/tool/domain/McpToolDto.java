package com.sirithree.shopops.admin.tool.domain;

public class McpToolDto {
    private String toolCode;
    private String toolName;
    private String category;
    private String description;
    private String inputSchema;
    private String outputSchema;
    private String permissionCode;
    private String riskLevel;
    private Boolean needApproval;
    private Boolean idempotent;
    private Integer timeoutMs;
    private Integer retryCount;
    private Boolean enabled;
    private String version;
    private String providerType;
    private String mcpServerCode;
    private String remoteToolName;
    private String schemaHash;
    private String remoteVersion;
    private String discoveryStatus;

    public McpToolDto(String toolCode, String toolName, String category, String permissionCode, String riskLevel) {
        this.toolCode = toolCode;
        this.toolName = toolName;
        this.category = category;
        this.permissionCode = permissionCode;
        this.riskLevel = riskLevel;
        this.needApproval = false;
        this.idempotent = false;
        this.timeoutMs = 5000;
        this.retryCount = 0;
        this.enabled = true;
        this.version = "1.0.0";
        this.providerType = "LOCAL";
        this.discoveryStatus = "READY";
    }

    public String getToolCode() { return toolCode; }
    public void setToolCode(String toolCode) { this.toolCode = toolCode; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInputSchema() { return inputSchema; }
    public void setInputSchema(String inputSchema) { this.inputSchema = inputSchema; }
    public String getOutputSchema() { return outputSchema; }
    public void setOutputSchema(String outputSchema) { this.outputSchema = outputSchema; }
    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getNeedApproval() { return needApproval; }
    public void setNeedApproval(Boolean needApproval) { this.needApproval = needApproval; }
    public Boolean getIdempotent() { return idempotent; }
    public void setIdempotent(Boolean idempotent) { this.idempotent = idempotent; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getMcpServerCode() { return mcpServerCode; }
    public void setMcpServerCode(String mcpServerCode) { this.mcpServerCode = mcpServerCode; }
    public String getRemoteToolName() { return remoteToolName; }
    public void setRemoteToolName(String remoteToolName) { this.remoteToolName = remoteToolName; }
    public String getSchemaHash() { return schemaHash; }
    public void setSchemaHash(String schemaHash) { this.schemaHash = schemaHash; }
    public String getRemoteVersion() { return remoteVersion; }
    public void setRemoteVersion(String remoteVersion) { this.remoteVersion = remoteVersion; }
    public String getDiscoveryStatus() { return discoveryStatus; }
    public void setDiscoveryStatus(String discoveryStatus) { this.discoveryStatus = discoveryStatus; }
}
