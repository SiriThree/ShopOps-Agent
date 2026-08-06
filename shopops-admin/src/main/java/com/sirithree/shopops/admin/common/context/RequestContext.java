package com.sirithree.shopops.admin.common.context;

import com.sirithree.shopops.admin.auth.domain.DataScope;
import java.util.List;
import java.util.Set;

public class RequestContext {
    private final String tokenId;
    private final Long tenantId;
    private final Long shopId;
    private final Long userId;
    private final String requestId;
    private final String traceId;
    private final String username;
    private final List<Long> accessibleShopIds;
    private final List<String> roles;
    private final Set<String> permissions;
    private final DataScope dataScope;
    private final String authType;
    private final boolean authenticated;

    public RequestContext(Long tenantId, Long shopId, Long userId, String requestId) {
        this(null, tenantId, shopId, userId, requestId, requestId, "user-" + userId,
                List.of(shopId), List.of("ADMIN"), Set.of(), DataScope.ASSIGNED_SHOPS, "HEADER", true);
    }

    public RequestContext(Long tenantId, Long shopId, Long userId, String requestId,
                          String username, List<String> roles, String authType, boolean authenticated) {
        this(null, tenantId, shopId, userId, requestId, requestId, username,
                shopId == null ? List.of() : List.of(shopId), roles, Set.of(), DataScope.ASSIGNED_SHOPS,
                authType, authenticated);
    }

    public RequestContext(String tokenId, Long tenantId, Long shopId, Long userId, String requestId,
                          String traceId, String username, List<Long> accessibleShopIds, List<String> roles,
                          Set<String> permissions, DataScope dataScope, String authType, boolean authenticated) {
        this.tokenId = tokenId;
        this.tenantId = tenantId;
        this.shopId = shopId;
        this.userId = userId;
        this.requestId = requestId;
        this.traceId = traceId;
        this.username = username;
        this.accessibleShopIds = accessibleShopIds == null ? List.of() : List.copyOf(accessibleShopIds);
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        this.dataScope = dataScope == null ? DataScope.ASSIGNED_SHOPS : dataScope;
        this.authType = authType;
        this.authenticated = authenticated;
    }

    public String getTokenId() { return tokenId; }
    public Long getTenantId() { return tenantId; }
    public Long getShopId() { return shopId; }
    public Long getCurrentShopId() { return shopId; }
    public Long getUserId() { return userId; }
    public String getRequestId() { return requestId; }
    public String getTraceId() { return traceId; }
    public String getUsername() { return username; }
    public List<Long> getAccessibleShopIds() { return accessibleShopIds; }
    public List<String> getRoles() { return roles; }
    public Set<String> getPermissions() { return permissions; }
    public DataScope getDataScope() { return dataScope; }
    public String getAuthType() { return authType; }
    public boolean isAuthenticated() { return authenticated; }
    public boolean hasRole(String role) { return roles.contains(role); }
    public boolean hasPermission(String permission) { return permissions.contains(permission); }
}
