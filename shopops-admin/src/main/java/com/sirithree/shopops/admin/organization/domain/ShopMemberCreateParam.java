package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ShopMemberCreateParam {
    @NotNull
    private Long userId;
    @NotBlank
    @Size(max = 64)
    private String roleCode;
    @NotBlank
    @Size(max = 32)
    private String status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
