package com.sirithree.shopops.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
        properties = {
                "shopops.persistence=memory",
                "shopops.auth.header-dev-mode=false"
        }
)
class AuthHeaderStrictModeIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRejectHeaderIdentityWhenHeaderDevModeIsDisabled() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Roles", "ADMIN");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAllowLoginAndBearerTokenWhenHeaderDevModeIsDisabled() {
        Map<String, Object> loginRequest = Map.of(
                "username", "admin",
                "password", "shopops123",
                "tenantId", 1,
                "shopId", 1
        );

        ResponseEntity<Map> loginResponse = restTemplate.exchange(
                url("/api/admin/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                Map.class
        );

        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().get("data");
        String accessToken = loginData.get("accessToken").toString();
        assertThat(accessToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(meResponse.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> meData = (Map<String, Object>) meResponse.getBody().get("data");
        assertThat(meData.get("userId")).isEqualTo(1);
        assertThat(meData.get("username")).isEqualTo("admin");
        assertThat((List<String>) meData.get("roles")).contains("ADMIN");
        assertThat(meData.get("authType")).isEqualTo("BEARER");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
