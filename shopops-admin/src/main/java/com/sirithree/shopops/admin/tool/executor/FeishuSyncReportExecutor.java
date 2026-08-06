package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.config.FeishuSyncProperties;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeishuSyncReportExecutor extends PortfolioOperationToolExecutor {
    private final FeishuSyncProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public FeishuSyncReportExecutor(FeishuSyncProperties properties) {
        this(properties, HttpClient.newHttpClient());
    }

    FeishuSyncReportExecutor(FeishuSyncProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public String toolCode() {
        return "feishu.sync_report";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        if (!enabled()) {
            data.put("documentId", "FEISHU-DEMO-DOC-001");
            data.put("documentUrl", "https://feishu.example.com/docx/shopops-demo");
            data.put("status", "SYNCED");
            data.put("mode", "demo-connector");
            return data;
        }

        long started = System.currentTimeMillis();
        String payload = webhookPayload(context, input);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getWebhookUrl()))
                .timeout(Duration.ofMillis(Math.max(1, properties.getTimeoutMs())))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Feishu webhook sync failed with HTTP " + response.statusCode());
        }

        data.put("documentId", "FEISHU-WEBHOOK-" + input.getOrDefault("reportId", "UNKNOWN"));
        data.put("documentUrl", input.getOrDefault("documentUrl", ""));
        data.put("status", "SYNCED");
        data.put("mode", "feishu-webhook");
        data.put("webhookStatusCode", response.statusCode());
        data.put("webhookUrlMasked", mask(properties.getWebhookUrl()));
        data.put("durationMs", System.currentTimeMillis() - started);
        return data;
    }

    private boolean enabled() {
        return properties != null
                && properties.isEnabled()
                && properties.getWebhookUrl() != null
                && !properties.getWebhookUrl().isBlank();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Feishu webhook sync failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Feishu webhook sync interrupted", e);
        }
    }

    private String webhookPayload(ToolInvokeContext context, Map<String, Object> input) {
        String text = "ShopOps report synced"
                + " tenantId=" + context.getTenantId()
                + " shopId=" + input.getOrDefault("shopId", context.getShopId())
                + " reportId=" + input.getOrDefault("reportId", "UNKNOWN")
                + " taskId=" + context.getTaskId()
                + " traceId=" + nullToEmpty(context.getTraceId());
        return "{\"msg_type\":\"text\",\"content\":{\"text\":\"" + escapeJson(text) + "\"}}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String mask(String value) {
        int index = value.lastIndexOf('/');
        if (index < 0 || index == value.length() - 1) {
            return "***";
        }
        return value.substring(0, index + 1) + "***";
    }
}
