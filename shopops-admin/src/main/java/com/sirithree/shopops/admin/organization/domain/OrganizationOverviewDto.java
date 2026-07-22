package com.sirithree.shopops.admin.organization.domain;

public class OrganizationOverviewDto {
    private Long tenantTotal;
    private Long shopTotal;
    private Long userTotal;
    private Long activeMemberTotal;
    private Long disabledMemberTotal;

    public Long getTenantTotal() { return tenantTotal; }
    public void setTenantTotal(Long tenantTotal) { this.tenantTotal = tenantTotal; }
    public Long getShopTotal() { return shopTotal; }
    public void setShopTotal(Long shopTotal) { this.shopTotal = shopTotal; }
    public Long getUserTotal() { return userTotal; }
    public void setUserTotal(Long userTotal) { this.userTotal = userTotal; }
    public Long getActiveMemberTotal() { return activeMemberTotal; }
    public void setActiveMemberTotal(Long activeMemberTotal) { this.activeMemberTotal = activeMemberTotal; }
    public Long getDisabledMemberTotal() { return disabledMemberTotal; }
    public void setDisabledMemberTotal(Long disabledMemberTotal) { this.disabledMemberTotal = disabledMemberTotal; }
}
