package com.sirithree.shopops.mcp.commerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shopops.mcp.commerce")
public class CommerceMcpServerProperties {
    private String bearerToken = "";
    private String serverName = "shopops-commerce-mcp-server";
    private String serverVersion = "0.1.0";
    private int maxDateRangeDays = 90;

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public int getMaxDateRangeDays() {
        return maxDateRangeDays;
    }

    public void setMaxDateRangeDays(int maxDateRangeDays) {
        this.maxDateRangeDays = maxDateRangeDays;
    }
}
