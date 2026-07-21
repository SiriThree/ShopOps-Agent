package com.sirithree.shopops.admin.organization.service.impl;

import com.sirithree.shopops.admin.auth.service.PasswordHashService;
import com.sirithree.shopops.admin.organization.domain.OrganizationOverviewDto;
import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopConfigDto;
import com.sirithree.shopops.admin.organization.domain.ShopConfigUpsertParam;
import com.sirithree.shopops.admin.organization.domain.ShopDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberCreateParam;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.ShopUpsertParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import com.sirithree.shopops.admin.organization.domain.TenantUpsertParam;
import com.sirithree.shopops.admin.organization.domain.UserCreateParam;
import com.sirithree.shopops.admin.organization.domain.UserPasswordResetParam;
import com.sirithree.shopops.admin.organization.service.OrganizationAdminService;
import com.sirithree.shopops.admin.persistence.mapper.OrganizationAdminMapper;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcOrganizationAdminService implements OrganizationAdminService {
    private final OrganizationAdminMapper mapper;
    private final PasswordHashService passwordHashService;

    public JdbcOrganizationAdminService(OrganizationAdminMapper mapper, PasswordHashService passwordHashService) {
        this.mapper = mapper;
        this.passwordHashService = passwordHashService;
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
    public CommonPage<ShopDto> listShops(Long tenantId, OrganizationQueryParam query) {
        List<ShopDto> shops = mapper.listShops(
                tenantId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()), query.offset(), query.safePageSize());
        Long total = mapper.countShops(tenantId, blankToNull(query.getKeyword()), blankToNull(query.getStatus()));
        return CommonPage.of(shops, query.safePageNum(), query.safePageSize(), zero(total));
    }

    @Override
    public CommonPage<ShopConfigDto> listShopConfigs(Long tenantId, Long shopId, OrganizationQueryParam query) {
        if (mapper.findShop(tenantId, shopId) == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        List<ShopConfigDto> configs = mapper.listShopConfigs(
                tenantId, shopId, blankToNull(query.getKeyword()), query.offset(), query.safePageSize());
        Long total = mapper.countShopConfigs(tenantId, shopId, blankToNull(query.getKeyword()));
        return CommonPage.of(configs, query.safePageNum(), query.safePageSize(), zero(total));
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

    @Override
    public OrganizationUserDto createUser(Long tenantId, Long shopId, UserCreateParam param) {
        String username = required(param.getUsername(), "用户名");
        if (zero(mapper.countUsername(username)) > 0) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        String tenantRole = normalizeTenantRole(param.getTenantRole());
        String shopRole = normalizeRoleCode(param.getShopRole());
        String status = normalizeStatus(param.getStatus());
        OrganizationUserDto user = new OrganizationUserDto();
        user.setUsername(username);
        user.setDisplayName(blankToNull(param.getDisplayName()));
        user.setPhone(blankToNull(param.getPhone()));
        user.setEmail(blankToNull(param.getEmail()));
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        mapper.insertUser(user, passwordHashService.hash(param.getPassword()));
        mapper.insertTenantMember(tenantId, user.getUserId(), tenantRole, status);
        mapper.insertShopMember(tenantId, shopId, user.getUserId(), shopRole, status);
        OrganizationUserDto created = mapper.findUser(tenantId, user.getUserId());
        created.setTenantRoles(mapper.listTenantRoles(tenantId, created.getUserId()));
        created.setShopRoles(mapper.listShopRoles(tenantId, shopId, created.getUserId()));
        return created;
    }

    @Override
    public OrganizationUserDto resetUserPassword(Long tenantId, Long shopId, Long userId, UserPasswordResetParam param) {
        required(param.getPassword(), "新密码");
        OrganizationUserDto user = mapper.findUser(tenantId, userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        int updated = mapper.updateUserPassword(userId, passwordHashService.hash(param.getPassword()));
        if (updated == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setTenantRoles(mapper.listTenantRoles(tenantId, userId));
        user.setShopRoles(mapper.listShopRoles(tenantId, shopId, userId));
        return user;
    }

    @Override
    public TenantDto createTenant(TenantUpsertParam param) {
        String tenantNo = required(param.getTenantNo(), "租户编号");
        if (zero(mapper.countTenantNoExcept(tenantNo, 0L)) > 0) {
            throw new IllegalArgumentException("租户编号已存在: " + tenantNo);
        }
        TenantDto tenant = new TenantDto();
        fillTenant(tenant, param);
        mapper.insertTenant(tenant);
        return mapper.findTenant(tenant.getTenantId());
    }

    @Override
    public TenantDto updateTenant(Long tenantId, TenantUpsertParam param) {
        TenantDto tenant = mapper.findTenant(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        String tenantNo = required(param.getTenantNo(), "租户编号");
        if (zero(mapper.countTenantNoExcept(tenantNo, tenantId)) > 0) {
            throw new IllegalArgumentException("租户编号已存在: " + tenantNo);
        }
        fillTenant(tenant, param);
        mapper.updateTenant(tenant);
        return mapper.findTenant(tenantId);
    }

    @Override
    public ShopDto createShop(Long tenantId, ShopUpsertParam param) {
        String shopNo = required(param.getShopNo(), "店铺编号");
        if (zero(mapper.countShopNoExcept(tenantId, shopNo, 0L)) > 0) {
            throw new IllegalArgumentException("店铺编号已存在: " + shopNo);
        }
        if (mapper.findUser(tenantId, param.getOwnerId()) == null) {
            throw new IllegalArgumentException("店铺负责人不存在");
        }
        ShopDto shop = new ShopDto();
        shop.setTenantId(tenantId);
        fillShop(shop, param);
        mapper.insertShop(shop);
        return mapper.findShop(tenantId, shop.getShopId());
    }

    @Override
    public ShopDto updateShop(Long tenantId, Long shopId, ShopUpsertParam param) {
        ShopDto shop = mapper.findShop(tenantId, shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        String shopNo = required(param.getShopNo(), "店铺编号");
        if (zero(mapper.countShopNoExcept(tenantId, shopNo, shopId)) > 0) {
            throw new IllegalArgumentException("店铺编号已存在: " + shopNo);
        }
        if (mapper.findUser(tenantId, param.getOwnerId()) == null) {
            throw new IllegalArgumentException("店铺负责人不存在");
        }
        fillShop(shop, param);
        mapper.updateShop(shop);
        return mapper.findShop(tenantId, shopId);
    }

    @Override
    public ShopMemberDto addShopMember(Long tenantId, Long shopId, ShopMemberCreateParam param) {
        if (mapper.findShop(tenantId, shopId) == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        if (mapper.findUser(tenantId, param.getUserId()) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (zero(mapper.countShopMemberByUser(tenantId, shopId, param.getUserId())) > 0) {
            throw new IllegalArgumentException("店铺成员已存在");
        }
        mapper.insertShopMember(tenantId, shopId, param.getUserId(),
                normalizeRoleCode(param.getRoleCode()), normalizeStatus(param.getStatus()));
        return mapper.findShopMemberByUser(tenantId, shopId, param.getUserId());
    }

    @Override
    public ShopConfigDto saveShopConfig(Long tenantId, Long shopId, Long userId, ShopConfigUpsertParam param) {
        if (mapper.findShop(tenantId, shopId) == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        String configKey = required(param.getConfigKey(), "配置键");
        mapper.upsertShopConfig(tenantId, shopId, configKey,
                required(param.getConfigValue(), "配置值"), normalizeValueType(param.getValueType()), userId);
        return mapper.findShopConfig(tenantId, shopId, configKey);
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

    private String normalizeTenantRole(String roleCode) {
        String value = roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("TENANT_ADMIN", "TENANT_OPERATOR", "TENANT_VIEWER").contains(value)) {
            throw new IllegalArgumentException("不支持的租户角色: " + roleCode);
        }
        return value;
    }

    private void fillTenant(TenantDto tenant, TenantUpsertParam param) {
        tenant.setTenantNo(required(param.getTenantNo(), "租户编号"));
        tenant.setTenantName(required(param.getTenantName(), "租户名称"));
        tenant.setStatus(normalizeStatus(param.getStatus()));
        tenant.setPlanType(blankToNull(param.getPlanType()));
        tenant.setContactName(blankToNull(param.getContactName()));
        tenant.setContactPhone(blankToNull(param.getContactPhone()));
    }

    private void fillShop(ShopDto shop, ShopUpsertParam param) {
        shop.setShopNo(required(param.getShopNo(), "店铺编号"));
        shop.setShopName(required(param.getShopName(), "店铺名称"));
        shop.setPlatformType(required(param.getPlatformType(), "平台类型"));
        if (param.getOwnerId() == null) {
            throw new IllegalArgumentException("店铺负责人不能为空");
        }
        shop.setOwnerId(param.getOwnerId());
        shop.setStatus(normalizeStatus(param.getStatus()));
    }

    private String normalizeValueType(String valueType) {
        String value = valueType == null ? "" : valueType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("string", "number", "boolean", "json").contains(value)) {
            throw new IllegalArgumentException("不支持的配置类型: " + valueType);
        }
        return value;
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long zero(Long value) {
        return value == null ? 0L : value;
    }
}
