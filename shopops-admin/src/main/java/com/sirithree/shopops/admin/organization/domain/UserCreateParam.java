package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateParam {
    @NotBlank
    @Size(max = 64)
    private String username;
    @Size(max = 128)
    private String displayName;
    @Size(max = 32)
    private String phone;
    @Size(max = 128)
    private String email;
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
    @NotBlank
    @Size(max = 64)
    private String tenantRole;
    @NotBlank
    @Size(max = 64)
    private String shopRole;
    @NotBlank
    @Size(max = 32)
    private String status;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTenantRole() { return tenantRole; }
    public void setTenantRole(String tenantRole) { this.tenantRole = tenantRole; }
    public String getShopRole() { return shopRole; }
    public void setShopRole(String shopRole) { this.shopRole = shopRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
