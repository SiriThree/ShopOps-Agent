package com.sirithree.shopops.admin.organization.service;

import com.sirithree.shopops.admin.organization.domain.OrganizationOverviewDto;
import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberCreateParam;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.ShopUpsertParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import com.sirithree.shopops.admin.organization.domain.TenantUpsertParam;
import com.sirithree.shopops.admin.organization.domain.UserCreateParam;
import com.sirithree.shopops.admin.organization.domain.UserPasswordResetParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface OrganizationAdminService {
    OrganizationOverviewDto overview(Long tenantId, Long shopId);

    CommonPage<OrganizationUserDto> listUsers(Long tenantId, Long shopId, OrganizationQueryParam query);

    CommonPage<TenantDto> listTenants(Long tenantId, OrganizationQueryParam query);

    CommonPage<ShopDto> listShops(Long tenantId, OrganizationQueryParam query);

    CommonPage<ShopMemberDto> listShopMembers(Long tenantId, Long shopId, OrganizationQueryParam query);

    ShopMemberDto updateShopMember(Long tenantId, Long shopId, Long memberId, ShopMemberUpdateParam param);

    OrganizationUserDto createUser(Long tenantId, Long shopId, UserCreateParam param);

    OrganizationUserDto resetUserPassword(Long tenantId, Long shopId, Long userId, UserPasswordResetParam param);

    TenantDto createTenant(TenantUpsertParam param);

    TenantDto updateTenant(Long tenantId, TenantUpsertParam param);

    ShopDto createShop(Long tenantId, ShopUpsertParam param);

    ShopDto updateShop(Long tenantId, Long shopId, ShopUpsertParam param);

    ShopMemberDto addShopMember(Long tenantId, Long shopId, ShopMemberCreateParam param);
}
