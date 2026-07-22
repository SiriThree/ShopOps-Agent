package com.sirithree.shopops.admin.connector.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConnectorSyncJobCreateParam {
    @NotBlank(message = "连接器编码不能为空")
    @Size(max = 80, message = "连接器编码不能超过80个字符")
    private String connectorCode;

    @Size(max = 200, message = "同步备注不能超过200个字符")
    private String remark;

    public String getConnectorCode() {
        return connectorCode;
    }

    public void setConnectorCode(String connectorCode) {
        this.connectorCode = connectorCode;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
