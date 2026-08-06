package com.sirithree.shopops.admin.connector.domain;

public class ConnectorCredentialTestResult {
    private String connectorCode;
    private boolean success;
    private String status;
    private String message;
    private String testedAt;

    public String getConnectorCode() {
        return connectorCode;
    }

    public void setConnectorCode(String connectorCode) {
        this.connectorCode = connectorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(String testedAt) {
        this.testedAt = testedAt;
    }
}
