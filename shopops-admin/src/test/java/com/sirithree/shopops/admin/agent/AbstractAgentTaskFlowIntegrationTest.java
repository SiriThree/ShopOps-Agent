package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

abstract class AbstractAgentTaskFlowIntegrationTest {
    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Map<String, Object> createDailyReviewTask() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");

        Map<String, Object> request = Map.of(
                "taskType", "daily_review",
                "userInput", "帮我生成今天店铺运营复盘",
                "dateRange", Map.of("start", "2026-07-18", "end", "2026-07-18")
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/agent/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(200);
        return castMap(body.get("data"));
    }

    protected Map get(String path) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url(path), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    protected Map post(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");

        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    protected Map post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");

        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    protected Map<String, Object> dataOf(Map response) {
        return castMap(dataOfObject(response));
    }

    protected Object dataOfObject(Map response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return response.get("data");
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    protected Map<String, Object> mapValue(Object value) {
        if (value instanceof String json) {
            try {
                return objectMapper.readValue(json, new TypeReference<>() {
                });
            } catch (Exception ex) {
                throw new IllegalArgumentException("JSON 对象解析失败", ex);
            }
        }
        return castMap(value);
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }
}
