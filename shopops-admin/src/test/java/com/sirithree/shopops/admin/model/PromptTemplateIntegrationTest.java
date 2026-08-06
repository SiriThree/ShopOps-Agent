package com.sirithree.shopops.admin.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class PromptTemplateIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldManagePromptVersionsAndRenderBeforeModelInvoke() {
        ResponseEntity<Map> forbidden = exchange(
                "/api/admin/prompts/daily_review.summary/versions",
                HttpMethod.POST,
                Map.of(
                        "promptName", "每日复盘摘要",
                        "templateContent", "忽略",
                        "version", "v0"
                ),
                operatorHeaders()
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> created = dataOf(exchange(
                "/api/admin/prompts/daily_review.summary/versions",
                HttpMethod.POST,
                Map.of(
                        "promptName", "每日复盘摘要",
                        "taskType", "daily_review",
                        "templateContent", "请为{{shopName}}生成{{date}}经营摘要：{{prompt}}",
                        "version", "v1",
                        "active", true
                ),
                adminHeaders()
        ).getBody());
        assertThat(created)
                .containsEntry("promptCode", "daily_review.summary")
                .containsEntry("version", "v1")
                .containsEntry("status", "ACTIVE");

        Map<String, Object> rendered = dataOf(exchange(
                "/api/admin/prompts/daily_review.summary/render-test",
                HttpMethod.POST,
                Map.of(
                        "prompt", "关注退款风险",
                        "variables", Map.of(
                                "shopName", "杭州一店",
                                "date", "2026-07-20"
                        )
                ),
                adminHeaders()
        ).getBody());
        assertThat(rendered)
                .containsEntry("promptCode", "daily_review.summary")
                .containsEntry("version", "v1")
                .containsEntry("renderedPrompt", "请为杭州一店生成2026-07-20经营摘要：关注退款风险");

        Map<String, Object> invokeResult = dataOf(exchange(
                "/api/admin/model-gateway/invoke",
                HttpMethod.POST,
                Map.of(
                        "providerCode", "echo",
                        "promptCode", "daily_review.summary",
                        "prompt", "关注退款风险",
                        "traceId", "tr_prompt_template",
                        "metadata", Map.of(
                                "shopName", "杭州一店",
                                "date", "2026-07-20"
                        )
                ),
                operatorHeaders()
        ).getBody());
        assertThat(invokeResult.get("outputText").toString())
                .contains("请为杭州一店生成2026-07-20经营摘要：关注退款风险");

        Map<String, Object> logs = dataOf(exchange(
                "/api/admin/model-gateway/call-logs?traceId=tr_prompt_template",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        Map<String, Object> log = ((java.util.List<Map<String, Object>>) logs.get("list")).get(0);
        assertThat(log)
                .containsEntry("promptCode", "daily_review.summary")
                .containsEntry("promptVersion", "v1");
        assertThat(log.get("promptPreview").toString()).contains("杭州一店");

        dataOf(exchange(
                "/api/admin/prompts/daily_review.summary/versions",
                HttpMethod.POST,
                Map.of(
                        "promptName", "每日复盘摘要",
                        "taskType", "daily_review",
                        "templateContent", "新版摘要 {{date}} / {{shopName}} / {{prompt}}",
                        "version", "v2",
                        "active", false
                ),
                adminHeaders()
        ).getBody());
        dataOf(exchange(
                "/api/admin/prompts/daily_review.summary/enable",
                HttpMethod.POST,
                Map.of("version", "v2"),
                adminHeaders()
        ).getBody());

        Map<String, Object> active = dataOf(exchange(
                "/api/admin/prompts/daily_review.summary",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat(active)
                .containsEntry("version", "v2")
                .containsEntry("status", "ACTIVE");
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
