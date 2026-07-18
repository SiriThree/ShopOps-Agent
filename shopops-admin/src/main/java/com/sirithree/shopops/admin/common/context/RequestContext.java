package com.sirithree.shopops.admin.common.context;

import java.util.List;

public class RequestContext {
    private final String tokenId;
    private final Long tenantId;
    private final Long shopId;
    private final Long userId;
    private final String requestId;
    private final String username;
    private final List<String> roles;
    private final String authType;
    private final boolean authenticated;

    public RequestContext(Long tenantId, Long shopId, Long userId, String requestId) {
        this(null, tenantId, shopId, userId, requestId, "user-" + userId, List.of("ADMIN"), "HEADER", true);
    }

    public RequestContext(Long tenantId, Long shopId, Long userId, String requestId,
                          String username, List<String> roles, String authType, boolean authenticated) {
        this(null, tenantId, shopId, userId, requestId, username, roles, authType, authenticated);
    }

    public RequestContext(String tokenId, Long tenantId, Long shopId, Long userId, String requestId,
                          String username, List<String> roles, String authType, boolean authenticated) {
        this.tokenId = tokenId;
        this.tenantId = tenantId;
        this.shopId = shopId;
        this.userId = userId;
        this.requestId = requestId;
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.authType = authType;
        this.authenticated = authenticated;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getShopId() {
        return shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getAuthType() {
        return authType;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
