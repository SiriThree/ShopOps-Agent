package com.sirithree.shopops.admin.auth.domain;

import com.sirithree.shopops.admin.common.context.RequestContext;
import java.util.List;
import java.util.Set;

public class CurrentUserDto {
    private Long tenantId;
    private Long shopId;
    private Long userId;
    private String username;
    private List<String> roles;
    private List<Long> accessibleShopIds;
    private Set<String> permissions;
    private String dataScope;
    private String traceId;
    private String authType;
    private boolean authenticated;
    private String requestId;

    public static CurrentUserDto from(RequestContext context) {
        CurrentUserDto dto = new CurrentUserDto();
        dto.setTenantId(context.getTenantId());
        dto.setShopId(context.getShopId());
        dto.setUserId(context.getUserId());
        dto.setUsername(context.getUsername());
        dto.setRoles(context.getRoles());
        dto.setAccessibleShopIds(context.getAccessibleShopIds());
        dto.setPermissions(context.getPermissions());
        dto.setDataScope(context.getDataScope().name());
        dto.setTraceId(context.getTraceId());
        dto.setAuthType(context.getAuthType());
        dto.setAuthenticated(context.isAuthenticated());
        dto.setRequestId(context.getRequestId());
        return dto;
    }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<Long> getAccessibleShopIds() { return accessibleShopIds; }
    public void setAccessibleShopIds(List<Long> accessibleShopIds) { this.accessibleShopIds = accessibleShopIds; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
