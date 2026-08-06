package com.sirithree.shopops.admin.model.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shopops.model-gateway.resilience")
public class ModelGatewayResilienceProperties {
    private boolean fallbackEnabled = true;
    private String fallbackProviderCode = "echo";

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public String getFallbackProviderCode() {
        return fallbackProviderCode;
    }

    public void setFallbackProviderCode(String fallbackProviderCode) {
        this.fallbackProviderCode = fallbackProviderCode;
    }
}
