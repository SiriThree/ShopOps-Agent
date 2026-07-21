package com.sirithree.shopops.admin.organization.service.impl;

import com.sirithree.shopops.admin.organization.domain.OrganizationOverviewDto;
import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import com.sirithree.shopops.admin.organization.service.OrganizationAdminService;
import com.sirithree.shopops.admin.persistence.mapper.OrganizationAdminMapper;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcOrganizationAdminService implements OrganizationAdminService {
    private final OrganizationAdminMapper mapper;

    public JdbcOrganizationAdminService(OrganizationAdminMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OrganizationOverviewDto overview(Long tenantId, Long shopId) {
        OrganizationOverviewDto overview = new OrganizationOverviewDto();
        overview.setTenantTotal(zero(mapper.tenantTotal(tenantId)));
        overview.setShopTotal(zero(mapper.shopTotal(tenantId)));
        overview.setUserTotal(zero(mapper.userTotal(tenantId)));
        overview.setActiveMemberTotal(zero(mapper.activeMemberTotal(tenantId, shopId)));
        overview.setDisabledMemberTotal(zero(mapper.disabledMemberTotal(tenantId, shopId)));
        return overview;
    }

    @Override
    public CommonPage<OrganizationUserDto> listUsers(Long tenantId, Long shopId, OrganizationQueryParam query) {
        List<OrganizationUserDto> users = mapper.listUsers(
                tenantId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()), query.offset(), query.safePageSize());
        users.forEach(user -> {
            user.setTenantRoles(mapper.listTenantRoles(tenantId, user.getUserId()));
            user.setShopRoles(mapper.listShopRoles(tenantId, shopId, user.getUserId()));
        });
        Long total = mapper.countUsers(tenantId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()));
        return CommonPage.of(users, query.safePageNum(), query.safePageSize(), zero(total));
    }

    @Override
    public CommonPage<TenantDto> listTenants(Long tenantId, OrganizationQueryParam query) {
        List<TenantDto> tenants = mapper.listTenants(
                tenantId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()), query.offset(), query.safePageSize());
        Long total = mapper.countTenants(tenantId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()));
        return CommonPage.of(tenants, query.safePageNum(), query.safePageSize(), zero(total));
    }

    @Override
    public CommonPage<ShopMemberDto> listShopMembers(Long tenantId, Long shopId, OrganizationQueryParam query) {
        List<ShopMemberDto> members = mapper.listShopMembers(
                tenantId, shopId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()), query.offset(), query.safePageSize());
        Long total = mapper.countShopMembers(tenantId, shopId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()));
        return CommonPage.of(members, query.safePageNum(), query.safePageSize(), zero(total));
    }

    @Override
    public ShopMemberDto updateShopMember(Long tenantId, Long shopId, Long memberId, ShopMemberUpdateParam param) {
        String roleCode = normalizeRoleCode(param.getRoleCode());
        String status = normalizeStatus(param.getStatus());
        int updated = mapper.updateShopMember(tenantId, shopId, memberId, roleCode, status);
        if (updated == 0) {
            throw new IllegalArgumentException("店铺成员不存在");
        }
        return mapper.findShopMember(tenantId, shopId, memberId);
    }

    private String normalizeRoleCode(String roleCode) {
        String value = roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("SHOP_OWNER", "SHOP_ADMIN", "SHOP_OPERATOR", "SHOP_VIEWER").contains(value)) {
            throw new IllegalArgumentException("不支持的店铺角色: " + roleCode);
        }
        return value;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ENABLED", "DISABLED").contains(value)) {
            throw new IllegalArgumentException("不支持的成员状态: " + status);
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long zero(Long value) {
        return value == null ? 0L : value;
    }
}
