package com.sirithree.shopops.admin.tool.domain;

public class McpToolDto {
    private String toolCode;
    private String toolName;
    private String category;
    private String permissionCode;
    private String riskLevel;
    private Boolean needApproval;
    private Boolean enabled;
    private String version;

    public McpToolDto(String toolCode, String toolName, String category, String permissionCode, String riskLevel) {
        this.toolCode = toolCode;
        this.toolName = toolName;
        this.category = category;
        this.permissionCode = permissionCode;
        this.riskLevel = riskLevel;
        this.needApproval = false;
        this.enabled = true;
        this.version = "1.0.0";
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getNeedApproval() {
        return needApproval;
    }

    public void setNeedApproval(Boolean needApproval) {
        this.needApproval = needApproval;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
