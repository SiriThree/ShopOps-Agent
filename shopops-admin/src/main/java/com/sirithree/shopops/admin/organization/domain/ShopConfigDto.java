package com.sirithree.shopops.admin.organization.domain;

import java.time.LocalDateTime;

public class ShopConfigDto {
    private Long configId;
    private Long tenantId;
    private Long shopId;
    private String configKey;
    private String configValue;
    private String valueType;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
