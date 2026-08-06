package com.sirithree.shopops.admin.organization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantUpsertParam {
    @NotBlank
    @Size(max = 64)
    private String tenantNo;
    @NotBlank
    @Size(max = 128)
    private String tenantName;
    @NotBlank
    @Size(max = 32)
    private String status;
    @Size(max = 32)
    private String planType;
    @Size(max = 64)
    private String contactName;
    @Size(max = 32)
    private String contactPhone;

    public String getTenantNo() { return tenantNo; }
    public void setTenantNo(String tenantNo) { this.tenantNo = tenantNo; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
