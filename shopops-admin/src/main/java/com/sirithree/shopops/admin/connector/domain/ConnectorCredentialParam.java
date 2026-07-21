package com.sirithree.shopops.admin.connector.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConnectorCredentialParam {
    @NotBlank(message = "连接器编码不能为空")
    @Size(max = 80, message = "连接器编码不能超过80个字符")
    private String connectorCode;

    @NotBlank(message = "凭证类型不能为空")
    @Size(max = 40, message = "凭证类型不能超过40个字符")
    private String credentialType;

    @NotBlank(message = "凭证密钥不能为空")
    @Size(max = 500, message = "凭证密钥不能超过500个字符")
    private String secretValue;

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

    public String getSecretValue() {
        return secretValue;
    }

    public void setSecretValue(String secretValue) {
        this.secretValue = secretValue;
    }
}
