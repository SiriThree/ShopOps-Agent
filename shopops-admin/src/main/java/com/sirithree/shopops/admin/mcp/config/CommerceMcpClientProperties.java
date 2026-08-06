package com.sirithree.shopops.admin.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shopops.mcp.servers.commerce")
public class CommerceMcpClientProperties {
    private String serverCode = "commerce-default";
    private String baseUrl = "http://localhost:8090";
    private String endpoint = "/mcp";
    private String bearerToken = "";
    private boolean enabled = false;
    private int connectTimeoutMs = 3000;
    private int requestTimeoutMs = 5000;

    public String getServerCode() { return serverCode; }
    public void setServerCode(String serverCode) { this.serverCode = serverCode; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getBearerToken() { return bearerToken; }
    public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
}
