package com.sirithree.shopops.admin.connector.domain;

public class ConnectorCredentialDto {
    private String connectorCode;
    private String credentialType;
    private String maskedSecret;
    private boolean configured;
    private boolean enabled;
    private String status;
    private String expiresAt;
    private String rotationStatus;
    private String rotationMessage;
    private Long daysUntilExpiry;
    private Long updatedBy;
    private String updatedAt;

    public String getConnectorCode() {
        return connectorCode;
    }

    public void setConnectorCode(String connectorCode) {
        this.connectorCode = connectorCode;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getMaskedSecret() {
        return maskedSecret;
    }

    public void setMaskedSecret(String maskedSecret) {
        this.maskedSecret = maskedSecret;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRotationStatus() {
        return rotationStatus;
    }

    public void setRotationStatus(String rotationStatus) {
        this.rotationStatus = rotationStatus;
    }

    public String getRotationMessage() {
        return rotationMessage;
    }

    public void setRotationMessage(String rotationMessage) {
        this.rotationMessage = rotationMessage;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
