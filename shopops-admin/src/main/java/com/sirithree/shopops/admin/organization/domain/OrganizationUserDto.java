package com.sirithree.shopops.admin.organization.domain;

import java.time.LocalDateTime;
import java.util.List;

public class OrganizationUserDto {
    private Long userId;
    private String username;
    private String displayName;
    private String email;
    private String phone;
    private String status;
    private List<String> tenantRoles;
    private List<String> shopRoles;
    private LocalDateTime createdAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getTenantRoles() { return tenantRoles; }
    public void setTenantRoles(List<String> tenantRoles) { this.tenantRoles = tenantRoles; }
    public List<String> getShopRoles() { return shopRoles; }
    public void setShopRoles(List<String> shopRoles) { this.shopRoles = shopRoles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
