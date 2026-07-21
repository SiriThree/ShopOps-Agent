package com.sirithree.shopops.admin.persistence.model;

import java.time.LocalDateTime;

public class ConnectorCredential {
    private Long id;
    private Long tenantId;
    private Long shopId;
    private String connectorCode;
    private String credentialType;
    private String encryptedSecret;
    private String secretPreview;
    private String status;
    private LocalDateTime expiresAt;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getConnectorCode() { return connectorCode; }
    public void setConnectorCode(String connectorCode) { this.connectorCode = connectorCode; }
    public String getCredentialType() { return credentialType; }
    public void setCredentialType(String credentialType) { this.credentialType = credentialType; }
    public String getEncryptedSecret() { return encryptedSecret; }
    public void setEncryptedSecret(String encryptedSecret) { this.encryptedSecret = encryptedSecret; }
    public String getSecretPreview() { return secretPreview; }
    public void setSecretPreview(String secretPreview) { this.secretPreview = secretPreview; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
