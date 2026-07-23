package com.sirithree.shopops.admin.tool.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.tool.config.FeishuSyncProperties;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FeishuSyncReportExecutorTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldPostReportMessageToConfiguredWebhook() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/webhook/test-token", exchange -> handleWebhook(exchange, requestBody));
        server.start();
        try {
            FeishuSyncProperties properties = new FeishuSyncProperties();
            properties.setEnabled(true);
            properties.setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/webhook/test-token");
            properties.setTimeoutMs(1000);

            FeishuSyncReportExecutor executor = new FeishuSyncReportExecutor(properties);
            ToolInvokeResult result = executor.execute(context(), Map.of(
                    "shopId", 7,
                    "reportId", 90001,
                    "documentUrl", "https://example.com/reports/90001"
            ));

            assertThat(result.getSuccess()).isTrue();
            Map<String, Object> data = (Map<String, Object>) result.getData();
            assertThat(data)
                    .containsEntry("status", "SYNCED")
                    .containsEntry("mode", "feishu-webhook")
                    .containsEntry("webhookStatusCode", 200)
                    .containsEntry("documentId", "FEISHU-WEBHOOK-90001")
                    .containsEntry("documentUrl", "https://example.com/reports/90001");
            assertThat(String.valueOf(data.get("webhookUrlMasked"))).endsWith("/webhook/***");
            assertThat(((Number) data.get("durationMs")).longValue()).isGreaterThanOrEqualTo(0L);
            assertThat(requestBody.get())
                    .contains("\"msg_type\":\"text\"")
                    .contains("tenantId=3")
                    .contains("shopId=7")
                    .contains("reportId=90001")
                    .contains("traceId=trace-feishu-test");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepDemoConnectorWhenWebhookIsNotConfigured() {
        FeishuSyncProperties properties = new FeishuSyncProperties();
        FeishuSyncReportExecutor executor = new FeishuSyncReportExecutor(properties);

        ToolInvokeResult result = executor.execute(context(), Map.of("shopId", 7, "reportId", 90001));

        assertThat(result.getSuccess()).isTrue();
        assertThat((Map<String, Object>) result.getData())
                .containsEntry("status", "SYNCED")
                .containsEntry("mode", "demo-connector")
                .containsEntry("documentId", "FEISHU-DEMO-DOC-001");
    }

    private void handleWebhook(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"code\":0}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private ToolInvokeContext context() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(3L);
        context.setShopId(7L);
        context.setUserId(11L);
        context.setTaskId(101L);
        context.setTraceId("trace-feishu-test");
        return context;
    }
}
