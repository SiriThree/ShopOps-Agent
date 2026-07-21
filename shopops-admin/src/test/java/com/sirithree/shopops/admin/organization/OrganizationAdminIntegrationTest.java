package com.sirithree.shopops.admin.organization;

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
        properties = "shopops.persistence=memory"
)
class OrganizationAdminIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldListOrganizationAndUpdateShopMemberByAdminOnly() {
        Map<String, Object> overview = dataOf(exchange(
                "/api/admin/organization/overview", HttpMethod.GET, null, operatorHeaders()).getBody());
        assertThat(overview)
                .containsEntry("tenantTotal", 1)
                .containsEntry("shopTotal", 1)
                .containsEntry("userTotal", 3)
                .containsEntry("activeMemberTotal", 3);

        Map<String, Object> users = dataOf(exchange(
                "/api/admin/organization/users?pageNum=1&pageSize=10", HttpMethod.GET, null, operatorHeaders()).getBody());
        List<Map<String, Object>> userList = (List<Map<String, Object>>) users.get("list");
        assertThat(userList)
                .extracting(user -> user.get("username"))
                .containsExactly("admin", "operator", "viewer");

        Map<String, Object> tenants = dataOf(exchange(
                "/api/admin/organization/tenants", HttpMethod.GET, null, operatorHeaders()).getBody());
        assertThat((List<Map<String, Object>>) tenants.get("list"))
                .extracting(tenant -> tenant.get("tenantName"))
                .containsExactly("演示租户");

        Map<String, Object> members = dataOf(exchange(
                "/api/admin/organization/shop-members?keyword=viewer", HttpMethod.GET, null, operatorHeaders()).getBody());
        List<Map<String, Object>> memberList = (List<Map<String, Object>>) members.get("list");
        assertThat(memberList).hasSize(1);
        assertThat(memberList.get(0))
                .containsEntry("username", "viewer")
                .containsEntry("roleCode", "SHOP_VIEWER");

        ResponseEntity<Map> forbidden = exchange(
                "/api/admin/organization/shop-members/3",
                HttpMethod.POST,
                Map.of("roleCode", "SHOP_OPERATOR", "status", "ENABLED"),
                operatorHeaders()
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> updated = dataOf(exchange(
                "/api/admin/organization/shop-members/3",
                HttpMethod.POST,
                Map.of("roleCode", "SHOP_OPERATOR", "status", "DISABLED"),
                adminHeaders()
        ).getBody());
        assertThat(updated)
                .containsEntry("username", "viewer")
                .containsEntry("roleCode", "SHOP_OPERATOR")
                .containsEntry("normalizedRole", "OPERATOR")
                .containsEntry("status", "DISABLED");

        Map<String, Object> audit = dataOf(exchange(
                "/api/admin/auth/audit-events?eventType=ORG_MEMBER_UPDATED&pageNum=1&pageSize=10",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat((List<Map<String, Object>>) audit.get("list"))
                .extracting(event -> event.get("eventType"))
                .contains("ORG_MEMBER_UPDATED");
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
