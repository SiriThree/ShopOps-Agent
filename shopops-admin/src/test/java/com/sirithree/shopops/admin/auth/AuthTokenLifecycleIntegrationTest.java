package com.sirithree.shopops.admin.auth;

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
        properties = {
                "shopops.persistence=memory",
                "shopops.auth.header-dev-mode=false"
        }
)
class AuthTokenLifecycleIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRevokeCurrentBearerTokenOnLogout() {
        String token = login("operator");
        assertThat(getMe(token).getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<Map> logoutResponse = restTemplate.exchange(
                url("/api/admin/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(token)),
                Map.class
        );

        assertThat(logoutResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(logoutResponse.getBody()).isNotNull();
        assertThat(logoutResponse.getBody().get("code")).isEqualTo(200);

        ResponseEntity<Map> meAfterLogout = getMe(token);
        assertThat(meAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(meAfterLogout.getBody()).isNotNull();
        assertThat(meAfterLogout.getBody().get("code")).isEqualTo(401);
    }

    @SuppressWarnings("unchecked")
    private String login(String username) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/admin/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "shopops123",
                        "tenantId", 1,
                        "shopId", 1
                )),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return data.get("accessToken").toString();
    }

    private ResponseEntity<Map> getMe(String token) {
        return restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                Map.class
        );
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
