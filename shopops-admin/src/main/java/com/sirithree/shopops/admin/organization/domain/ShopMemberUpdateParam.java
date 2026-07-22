package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;

public class ShopMemberUpdateParam {
    @NotBlank
    private String roleCode;
    @NotBlank
    private String status;

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
