package com.sirithree.shopops.admin.connector.domain;

public class ConnectorStatusDto {
    private String connectorCode;
    private String connectorName;
    private String category;
    private String propertyKey;
    private boolean configured;
    private boolean available;
    private String status;
    private String configuredPath;
    private String message;
    private String lastCheckedAt;

    public String getConnectorCode() {
        return connectorCode;
    }

    public void setConnectorCode(String connectorCode) {
        this.connectorCode = connectorCode;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfiguredPath() {
        return configuredPath;
    }

    public void setConfiguredPath(String configuredPath) {
        this.configuredPath = configuredPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(String lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
