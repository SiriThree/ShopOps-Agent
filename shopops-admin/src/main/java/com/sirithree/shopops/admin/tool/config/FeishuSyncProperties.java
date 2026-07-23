package com.sirithree.shopops.admin.tool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shopops.feishu.sync")
public class FeishuSyncProperties {
    private boolean enabled;
    private String webhookUrl;
    private int timeoutMs = 3000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
