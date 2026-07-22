package com.sirithree.shopops.admin.organization.domain;

import java.time.LocalDateTime;

public class ShopMemberDto {
    private Long memberId;
    private Long tenantId;
    private Long shopId;
    private String shopName;
    private Long userId;
    private String username;
    private String displayName;
    private String roleCode;
    private String normalizedRole;
    private String status;
    private LocalDateTime joinedAt;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getNormalizedRole() { return normalizedRole; }
    public void setNormalizedRole(String normalizedRole) { this.normalizedRole = normalizedRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
