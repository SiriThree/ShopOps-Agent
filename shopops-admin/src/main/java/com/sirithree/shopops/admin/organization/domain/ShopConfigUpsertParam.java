package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShopConfigUpsertParam {
    @NotBlank
    @Size(max = 128)
    private String configKey;
    @NotBlank
    private String configValue;
    @NotBlank
    @Size(max = 32)
    private String valueType;

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
}
