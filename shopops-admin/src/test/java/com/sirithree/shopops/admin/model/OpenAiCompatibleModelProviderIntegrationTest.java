package com.sirithree.shopops.admin.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=memory",
                "shopops.model-gateway.openai-compatible.enabled=true",
                "shopops.model-gateway.openai-compatible.api-key=test-key",
                "shopops.model-gateway.openai-compatible.default-model=test-model"
        }
)
class OpenAiCompatibleModelProviderIntegrationTest {
    private static final AtomicReference<String> requestBody = new AtomicReference<>();
    private static final AtomicReference<String> authorization = new AtomicReference<>();
    private static HttpServer modelServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void modelGatewayProperties(DynamicPropertyRegistry registry) {
        startModelServer();
        registry.add("shopops.model-gateway.openai-compatible.base-url",
                () -> "http://localhost:" + modelServer.getAddress().getPort() + "/v1");
    }

    @AfterAll
    static void stopModelServer() {
        if (modelServer != null) {
            modelServer.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldInvokeOpenAiCompatibleProviderAndRecordUsage() {
        ResponseEntity<Map> response = exchange(
                "/api/admin/model-gateway/invoke",
                HttpMethod.POST,
                Map.of(
                        "providerCode", "openai-compatible",
                        "prompt", "生成一段经营摘要",
                        "traceId", "tr_openai_compatible",
                        "metadata", Map.of(
                                "systemPrompt", "你是电商经营分析助手",
                                "temperature", 0.2,
                                "maxTokens", 128
                        )
                ),
                operatorHeaders()
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> result = dataOf(response.getBody());
        assertThat(result)
                .containsEntry("providerCode", "openai-compatible")
                .containsEntry("modelName", "test-model")
                .containsEntry("status", "SUCCESS")
                .containsEntry("outputText", "兼容模型返回成功");
        assertThat(result).containsEntry("promptTokens", 11)
                .containsEntry("completionTokens", 7)
                .containsEntry("totalTokens", 18);
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        assertThat(requestBody.get())
                .contains("\"model\":\"test-model\"")
                .contains("\"role\":\"system\"")
                .contains("\"content\":\"你是电商经营分析助手\"")
                .contains("\"role\":\"user\"")
                .contains("\"temperature\":0.2")
                .contains("\"max_tokens\":128");

        Map<String, Object> logs = dataOf(exchange(
                "/api/admin/model-gateway/call-logs?traceId=tr_openai_compatible",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat(logs.get("total")).isEqualTo(1);
        Map<String, Object> log = ((java.util.List<Map<String, Object>>) logs.get("list")).get(0);
        assertThat(log)
                .containsEntry("providerCode", "openai-compatible")
                .containsEntry("modelName", "test-model")
                .containsEntry("totalTokens", 18);
    }

    private static void startModelServer() {
        if (modelServer != null) {
            return;
        }
        try {
            modelServer = HttpServer.create(new InetSocketAddress(0), 0);
            modelServer.createContext("/v1/chat/completions", OpenAiCompatibleModelProviderIntegrationTest::handleModelRequest);
            modelServer.start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动模型模拟服务失败", ex);
        }
    }

    private static void handleModelRequest(HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = """
                {
                  "id": "chatcmpl-test",
                  "model": "test-model",
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "兼容模型返回成功"
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 11,
                    "completion_tokens": 7,
                    "total_tokens": 18
                  }
                }
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Map<String, Object> body, HttpHeaders headers) {
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return (Map<String, Object>) response.get("data");
    }

    private HttpHeaders adminHeaders() {
        return headers("1", "admin", "ADMIN");
    }

    private HttpHeaders operatorHeaders() {
        return headers("2", "operator", "OPERATOR");
    }

    private HttpHeaders headers(String userId, String username, String roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", userId);
        headers.set("X-User-Name", username);
        headers.set("X-User-Roles", roles);
        return headers;
    }
}
