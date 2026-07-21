package com.sirithree.shopops.admin.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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

        Map<String, Object> shops = dataOf(exchange(
                "/api/admin/organization/shops", HttpMethod.GET, null, operatorHeaders()).getBody());
        assertThat((List<Map<String, Object>>) shops.get("list"))
                .extracting(shop -> shop.get("shopNo"))
                .containsExactly("SHOP_DEFAULT");

        Map<String, Object> configs = dataOf(exchange(
                "/api/admin/organization/shops/1/configs", HttpMethod.GET, null, operatorHeaders()).getBody());
        assertThat((List<Map<String, Object>>) configs.get("list"))
                .extracting(config -> config.get("configKey"))
                .contains("refund_rate_warn_threshold", "negative_comment_warn_threshold");

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

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateUserResetPasswordAndManageTenantByAdmin() {
        Map<String, Object> createdUser = dataOf(exchange(
                "/api/admin/organization/users",
                HttpMethod.POST,
                Map.of(
                        "username", "planner",
                        "displayName", "计划员",
                        "email", "planner@shopops.local",
                        "phone", "13900000000",
                        "password", "shopops456",
                        "tenantRole", "TENANT_OPERATOR",
                        "shopRole", "SHOP_OPERATOR",
                        "status", "ENABLED"
                ),
                adminHeaders()
        ).getBody());
        assertThat(createdUser)
                .containsEntry("username", "planner")
                .containsEntry("displayName", "计划员");
        assertThat((List<String>) createdUser.get("tenantRoles")).containsExactly("TENANT_OPERATOR");
        assertThat((List<String>) createdUser.get("shopRoles")).containsExactly("SHOP_OPERATOR");

        Map<String, Object> resetUser = dataOf(exchange(
                "/api/admin/organization/users/" + createdUser.get("userId") + "/password",
                HttpMethod.POST,
                Map.of("password", "shopops789"),
                adminHeaders()
        ).getBody());
        assertThat(resetUser).containsEntry("username", "planner");

        Map<String, Object> createdTenant = dataOf(exchange(
                "/api/admin/organization/tenants",
                HttpMethod.POST,
                Map.of(
                        "tenantNo", "TENANT_NEW",
                        "tenantName", "新租户",
                        "status", "ENABLED",
                        "planType", "PRO",
                        "contactName", "负责人",
                        "contactPhone", "13812345678"
                ),
                adminHeaders()
        ).getBody());
        assertThat(createdTenant)
                .containsEntry("tenantNo", "TENANT_NEW")
                .containsEntry("tenantName", "新租户");

        Map<String, Object> updatedTenant = dataOf(exchange(
                "/api/admin/organization/tenants/" + createdTenant.get("tenantId"),
                HttpMethod.POST,
                Map.of(
                        "tenantNo", "TENANT_NEW",
                        "tenantName", "新租户已更新",
                        "status", "DISABLED",
                        "planType", "ENTERPRISE",
                        "contactName", "新负责人",
                        "contactPhone", "13887654321"
                ),
                adminHeaders()
        ).getBody());
        assertThat(updatedTenant)
                .containsEntry("tenantName", "新租户已更新")
                .containsEntry("status", "DISABLED");

        Map<String, Object> createdShop = dataOf(exchange(
                "/api/admin/organization/shops",
                HttpMethod.POST,
                Map.of(
                        "shopNo", "SHOP_NEW",
                        "shopName", "新店铺",
                        "platformType", "mock.mall",
                        "ownerId", 1,
                        "status", "ENABLED"
                ),
                adminHeaders()
        ).getBody());
        assertThat(createdShop)
                .containsEntry("shopNo", "SHOP_NEW")
                .containsEntry("shopName", "新店铺");

        Map<String, Object> updatedShop = dataOf(exchange(
                "/api/admin/organization/shops/" + createdShop.get("shopId"),
                HttpMethod.POST,
                Map.of(
                        "shopNo", "SHOP_NEW",
                        "shopName", "新店铺已更新",
                        "platformType", "mock.mall",
                        "ownerId", 1,
                        "status", "DISABLED"
                ),
                adminHeaders()
        ).getBody());
        assertThat(updatedShop)
                .containsEntry("shopName", "新店铺已更新")
                .containsEntry("status", "DISABLED");

        Map<String, Object> boundMember = dataOf(exchange(
                "/api/admin/organization/shops/" + createdShop.get("shopId") + "/members",
                HttpMethod.POST,
                Map.of(
                        "userId", createdUser.get("userId"),
                        "roleCode", "SHOP_OPERATOR",
                        "status", "ENABLED"
                ),
                adminHeaders()
        ).getBody());
        assertThat(boundMember)
                .containsEntry("username", "planner")
                .containsEntry("roleCode", "SHOP_OPERATOR");

        Map<String, Object> savedConfig = dataOf(exchange(
                "/api/admin/organization/shops/" + createdShop.get("shopId") + "/configs",
                HttpMethod.POST,
                Map.of(
                        "configKey", "agent_model_policy",
                        "configValue", "balanced",
                        "valueType", "string"
                ),
                adminHeaders()
        ).getBody());
        assertThat(savedConfig)
                .containsEntry("configKey", "agent_model_policy")
                .containsEntry("configValue", "balanced")
                .containsEntry("valueType", "string");

        Map<String, Object> audit = dataOf(exchange(
                "/api/admin/auth/audit-events?pageNum=1&pageSize=20",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat((List<Map<String, Object>>) audit.get("list"))
                .extracting(event -> event.get("eventType"))
                .contains("ORG_USER_CREATED", "ORG_USER_PASSWORD_RESET", "ORG_TENANT_CREATED", "ORG_TENANT_UPDATED",
                        "ORG_SHOP_CREATED", "ORG_SHOP_UPDATED", "ORG_SHOP_MEMBER_ADDED", "ORG_SHOP_CONFIG_SAVED");
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
