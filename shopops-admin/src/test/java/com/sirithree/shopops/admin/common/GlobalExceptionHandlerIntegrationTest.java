package com.sirithree.shopops.admin.common;

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
class GlobalExceptionHandlerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnUnifiedValidationResultWhenRequestBodyIsInvalid() {
        Map<String, Object> request = Map.of(
                "taskType", "daily_review",
                "userInput", "帮我生成今天店铺运营复盘"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/agent/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(request, defaultHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
        assertThat(response.getBody().get("message").toString()).contains("dateRange");
        assertThat(response.getBody().get("data")).isNull();
    }

    @Test
    void shouldReturnUnifiedValidationResultWhenHeaderTypeIsInvalid() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-Tenant-Id", "abc");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/tools"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
        assertThat(response.getBody().get("message").toString()).contains("X-Tenant-Id");
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
