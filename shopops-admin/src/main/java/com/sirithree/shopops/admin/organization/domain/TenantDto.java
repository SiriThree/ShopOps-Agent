package com.sirithree.shopops.admin.organization.domain;

import java.time.LocalDateTime;

public class TenantDto {
    private Long tenantId;
    private String tenantNo;
    private String tenantName;
    private String status;
    private String planType;
    private String contactName;
    private String contactPhone;
    private Long shopCount;
    private Long memberCount;
    private LocalDateTime createdAt;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
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
    public Long getShopCount() { return shopCount; }
    public void setShopCount(Long shopCount) { this.shopCount = shopCount; }
    public Long getMemberCount() { return memberCount; }
    public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
