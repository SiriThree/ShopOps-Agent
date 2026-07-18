package com.sirithree.shopops.admin.common.context;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RequestContextResolver {
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_SHOP_ID = "X-Shop-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    public RequestContext resolve(HttpServletRequest request) {
        Long tenantId = longHeader(request, HEADER_TENANT_ID, 1L);
        Long shopId = longHeader(request, HEADER_SHOP_ID, 1L);
        Long userId = longHeader(request, HEADER_USER_ID, 1L);
        String requestId = headerOrDefault(request, HEADER_REQUEST_ID, generateRequestId());
        String username = headerOrDefault(request, HEADER_USER_NAME, "user-" + userId);
        List<String> roles = roles(request.getHeader(HEADER_USER_ROLES));
        String authType = authType(request.getHeader(HEADER_AUTHORIZATION));
        return new RequestContext(tenantId, shopId, userId, requestId, username, roles, authType, true);
    }

    private Long longHeader(HttpServletRequest request, String name, Long defaultValue) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid request header type: " + name);
        }
    }

    private List<String> roles(String value) {
        if (value == null || value.isBlank()) {
            return List.of("ADMIN");
        }
        List<String> parsedRoles = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
        return parsedRoles.isEmpty() ? List.of("ADMIN") : parsedRoles;
    }

    private String authType(String authorization) {
        if (authorization != null && authorization.trim().startsWith("Bearer ")) {
            return "BEARER";
        }
        return "HEADER";
    }

    private String headerOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
