package com.sirithree.shopops.admin.organization.service;

import com.sirithree.shopops.admin.organization.domain.OrganizationOverviewDto;
import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import com.sirithree.shopops.common.api.CommonPage;

public interface OrganizationAdminService {
    OrganizationOverviewDto overview(Long tenantId, Long shopId);

    CommonPage<OrganizationUserDto> listUsers(Long tenantId, Long shopId, OrganizationQueryParam query);

    CommonPage<TenantDto> listTenants(Long tenantId, OrganizationQueryParam query);

    CommonPage<ShopMemberDto> listShopMembers(Long tenantId, Long shopId, OrganizationQueryParam query);

    ShopMemberDto updateShopMember(Long tenantId, Long shopId, Long memberId, ShopMemberUpdateParam param);
}
