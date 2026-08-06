package com.sirithree.shopops.admin.persistence.model;

import java.time.LocalDateTime;

public class McpTool {
    private Long id;
    private Long tenantId;
    private String toolCode;
    private String toolName;
    private String category;
    private String description;
    private String inputSchema;
    private String outputSchema;
    private String permissionCode;
    private String riskLevel;
    private Integer needApproval;
    private Integer idempotent;
    private Integer timeoutMs;
    private Integer retryCount;
    private String connectorCode;
    private Integer enabled;
    private String version;
    private String owner;
    private String providerType;
    private String mcpServerCode;
    private String remoteToolName;
    private String schemaHash;
    private String remoteVersion;
    private String discoveryStatus;
    private LocalDateTime lastDiscoveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
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
    public Integer getNeedApproval() { return needApproval; }
    public void setNeedApproval(Integer needApproval) { this.needApproval = needApproval; }
    public Integer getIdempotent() { return idempotent; }
    public void setIdempotent(Integer idempotent) { this.idempotent = idempotent; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getConnectorCode() { return connectorCode; }
    public void setConnectorCode(String connectorCode) { this.connectorCode = connectorCode; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
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
    public LocalDateTime getLastDiscoveredAt() { return lastDiscoveredAt; }
    public void setLastDiscoveredAt(LocalDateTime lastDiscoveredAt) { this.lastDiscoveredAt = lastDiscoveredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
