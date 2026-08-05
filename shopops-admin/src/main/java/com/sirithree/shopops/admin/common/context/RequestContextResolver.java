package com.sirithree.shopops.admin.common.context;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.DataScope;
import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.auth.domain.TokenPrincipal;
import com.sirithree.shopops.admin.auth.domain.UserRoleProfile;
import com.sirithree.shopops.admin.auth.exception.AuthenticationException;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.auth.service.TokenSessionService;
import com.sirithree.shopops.admin.auth.service.TokenService;
import com.sirithree.shopops.admin.auth.service.UserRoleService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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

    private final UserRoleService userRoleService;
    private final AuthorizationService authorizationService;
    private final TokenService tokenService;
    private final AuthAuditService authAuditService;
    private final TokenSessionService tokenSessionService;
    private final boolean headerDevMode;

    public RequestContextResolver(UserRoleService userRoleService,
                                  AuthorizationService authorizationService,
                                  TokenService tokenService,
                                  AuthAuditService authAuditService,
                                  TokenSessionService tokenSessionService,
                                  @Value("${shopops.auth.header-dev-mode:false}") boolean headerDevMode) {
        this.userRoleService = userRoleService;
        this.authorizationService = authorizationService;
        this.tokenService = tokenService;
        this.authAuditService = authAuditService;
        this.tokenSessionService = tokenSessionService;
        this.headerDevMode = headerDevMode;
    }

    public RequestContext resolve(HttpServletRequest request) {
        String requestId = headerOrDefault(request, HEADER_REQUEST_ID, generateRequestId());
        Optional<String> bearerToken = bearerToken(request);
        if (bearerToken.isPresent()) {
            Optional<TokenPrincipal> parsedPrincipal = tokenService.parse(bearerToken.get());
            if (parsedPrincipal.isEmpty()) {
                recordAuthenticationFailure(request, requestId, "BEARER", "Invalid bearer token");
                throw new AuthenticationException("Invalid bearer token");
            }
            TokenPrincipal principal = parsedPrincipal.get();
            if (!tokenSessionService.validateAndTouch(principal)) {
                recordAuthenticationFailure(request, requestId, "BEARER", "Token session is inactive");
                throw new AuthenticationException("Token session is inactive");
            }
            AuthorizationService.AuthorizationSnapshot authorization = authorizationService.resolve(
                    principal.getTenantId(), principal.getShopId(), principal.getUserId());
            List<String> roles = principal.getRoles() == null || principal.getRoles().isEmpty()
                    ? authorization.roles()
                    : principal.getRoles();
            return new RequestContext(
                    principal.getTokenId(), principal.getTenantId(), principal.getShopId(), principal.getUserId(),
                    requestId, requestId, principal.getUsername(), authorization.accessibleShopIds(),
                    roles, permissionsFor(roles), dataScopeFor(roles), "BEARER", true
            );
        }
        if (!headerDevMode) {
            if (isLoginRequest(request)) {
                return anonymousContext(requestId);
            }
            recordAuthenticationFailure(request, requestId, "BEARER", "Bearer token is required");
            throw new AuthenticationException("Bearer token is required");
        }

        Long tenantId = longHeader(request, HEADER_TENANT_ID, 1L);
        Long shopId = longHeader(request, HEADER_SHOP_ID, 1L);
        Long userId = longHeader(request, HEADER_USER_ID, 1L);
        AuthorizationService.AuthorizationSnapshot authorization = authorizationService.resolve(tenantId, shopId, userId);
        Optional<UserRoleProfile> userRoleProfile = userRoleService.getUserRoleProfile(tenantId, shopId, userId);
        String username = username(request, userId, userRoleProfile);
        List<String> roles = roles(request.getHeader(HEADER_USER_ROLES), userRoleProfile);
        return new RequestContext(null, tenantId, shopId, userId, requestId, requestId, username,
                authorization.accessibleShopIds(), roles, permissionsFor(roles), dataScopeFor(roles), "HEADER", true);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/admin/auth/login".equals(request.getRequestURI());
    }

    private RequestContext anonymousContext(String requestId) {
        return new RequestContext(null, 0L, 0L, 0L, requestId, requestId, "anonymous", List.of(), List.of(), java.util.Set.of(), com.sirithree.shopops.admin.auth.domain.DataScope.SELF_CREATED, "ANONYMOUS", false);
    }

    private void recordAuthenticationFailure(HttpServletRequest request, String requestId, String authType, String reason) {
        AuthAuditEventCreateCommand command = new AuthAuditEventCreateCommand();
        command.setTenantId(longHeaderOrDefault(request, HEADER_TENANT_ID, 0L));
        command.setShopId(longHeaderOrDefault(request, HEADER_SHOP_ID, 0L));
        command.setUserId(longHeaderOrDefault(request, HEADER_USER_ID, null));
        command.setUsername(request.getHeader(HEADER_USER_NAME));
        command.setEventType("AUTHENTICATION");
        command.setEventStatus("FAILURE");
        command.setAuthType(authType);
        command.setRequestId(requestId);
        command.setClientIp(clientIp(request));
        command.setUserAgent(request.getHeader("User-Agent"));
        command.setFailureReason(reason);
        authAuditService.record(command);
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HEADER_AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return Optional.empty();
        }
        String trimmed = authorization.trim();
        if (!trimmed.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = trimmed.substring("Bearer ".length()).trim();
        return token.isBlank() ? Optional.empty() : Optional.of(token);
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

    private Long longHeaderOrDefault(HttpServletRequest request, String name, Long defaultValue) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String username(HttpServletRequest request, Long userId, Optional<UserRoleProfile> userRoleProfile) {
        String usernameHeader = request.getHeader(HEADER_USER_NAME);
        if (usernameHeader != null && !usernameHeader.isBlank()) {
            return usernameHeader;
        }
        return userRoleProfile
                .map(UserRoleProfile::getUsername)
                .filter(username -> !username.isBlank())
                .orElse("user-" + userId);
    }

    private List<String> roles(String value, Optional<UserRoleProfile> userRoleProfile) {
        if (value == null || value.isBlank()) {
            return userRoleProfile
                    .map(UserRoleProfile::getRoles)
                    .filter(roles -> !roles.isEmpty())
                    .orElseGet(() -> List.of("ADMIN"));
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

    private Set<String> permissionsFor(List<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roles.contains("VIEWER") || roles.contains("OPERATOR") || roles.contains("ADMIN")) {
            permissions.addAll(Set.of(PermissionCode.DASHBOARD_READ, PermissionCode.ORDER_READ,
                    PermissionCode.PRODUCT_READ, PermissionCode.REVIEW_READ, PermissionCode.TASK_READ,
                    PermissionCode.APPROVAL_READ, PermissionCode.CONNECTOR_READ, PermissionCode.TOOL_READ,
                    "comment:read", "ad:read", "report:read"));
        }
        if (roles.contains("OPERATOR") || roles.contains("ADMIN")) {
            permissions.addAll(Set.of(PermissionCode.ORDER_EXPORT, PermissionCode.PRODUCT_UPDATE,
                    PermissionCode.REPORT_GENERATE, PermissionCode.TASK_CANCEL,
                    PermissionCode.TOOL_EXECUTE, PermissionCode.AGENT_EXECUTE,
                    "order:refund", "product:write", "ad:write", "report:export", "feishu:write"));
        }
        if (roles.contains("ADMIN")) {
            permissions.addAll(Set.of(PermissionCode.APPROVAL_REVIEW, PermissionCode.CONNECTOR_MANAGE,
                    PermissionCode.AUDIT_READ, PermissionCode.USER_MANAGE));
        }
        return Set.copyOf(permissions);
    }

    private DataScope dataScopeFor(List<String> roles) {
        return roles.contains("ADMIN") ? DataScope.ALL_TENANT : DataScope.ASSIGNED_SHOPS;
    }

    private String headerOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
