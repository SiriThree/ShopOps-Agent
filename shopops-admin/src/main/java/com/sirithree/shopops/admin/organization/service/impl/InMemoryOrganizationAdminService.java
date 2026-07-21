package com.sirithree.shopops.admin.organization.service.impl;

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
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryOrganizationAdminService implements OrganizationAdminService {
    private final List<OrganizationUserDto> users = seedUsers();
    private final List<TenantDto> tenants = seedTenants();
    private final List<ShopDto> shops = seedShops();
    private final Map<Long, ShopConfigDto> shopConfigs = seedShopConfigs();
    private final Map<Long, ShopMemberDto> shopMembers = seedShopMembers();
    private long nextUserId = 4L;
    private long nextTenantId = 2L;
    private long nextShopId = 2L;
    private long nextConfigId = 4L;
    private long nextMemberId = 4L;

    @Override
    public OrganizationOverviewDto overview(Long tenantId, Long shopId) {
        OrganizationOverviewDto overview = new OrganizationOverviewDto();
        overview.setTenantTotal((long) tenants.size());
        overview.setShopTotal(shops.stream().filter(shop -> tenantId.equals(shop.getTenantId())).count());
        overview.setUserTotal((long) users.size());
        overview.setActiveMemberTotal(shopMembers.values().stream().filter(member -> "ENABLED".equals(member.getStatus())).count());
        overview.setDisabledMemberTotal(shopMembers.values().stream().filter(member -> "DISABLED".equals(member.getStatus())).count());
        return overview;
    }

    @Override
    public CommonPage<OrganizationUserDto> listUsers(Long tenantId, Long shopId, OrganizationQueryParam query) {
        return page(users.stream()
                .filter(user -> matches(user.getUsername(), user.getDisplayName(), user.getEmail(), query.getKeyword()))
                .filter(user -> statusMatches(user.getStatus(), query.getStatus()))
                .toList(), query);
    }

    @Override
    public CommonPage<TenantDto> listTenants(Long tenantId, OrganizationQueryParam query) {
        return page(tenants.stream()
                .filter(tenant -> tenantId.equals(tenant.getTenantId()))
                .filter(tenant -> matches(tenant.getTenantNo(), tenant.getTenantName(), tenant.getContactName(), query.getKeyword()))
                .filter(tenant -> statusMatches(tenant.getStatus(), query.getStatus()))
                .toList(), query);
    }

    @Override
    public CommonPage<ShopDto> listShops(Long tenantId, OrganizationQueryParam query) {
        return page(shops.stream()
                .filter(shop -> tenantId.equals(shop.getTenantId()))
                .filter(shop -> matches(shop.getShopNo(), shop.getShopName(), shop.getPlatformType(), query.getKeyword()))
                .filter(shop -> statusMatches(shop.getStatus(), query.getStatus()))
                .toList(), query);
    }

    @Override
    public CommonPage<ShopConfigDto> listShopConfigs(Long tenantId, Long shopId, OrganizationQueryParam query) {
        ensureShop(tenantId, shopId);
        return page(shopConfigs.values().stream()
                .filter(config -> tenantId.equals(config.getTenantId()) && shopId.equals(config.getShopId()))
                .filter(config -> matches(config.getConfigKey(), config.getConfigValue(), config.getValueType(), query.getKeyword()))
                .toList(), query);
    }

    @Override
    public CommonPage<ShopMemberDto> listShopMembers(Long tenantId, Long shopId, OrganizationQueryParam query) {
        return page(shopMembers.values().stream()
                .filter(member -> tenantId.equals(member.getTenantId()) && shopId.equals(member.getShopId()))
                .filter(member -> matches(member.getUsername(), member.getDisplayName(), member.getRoleCode(), query.getKeyword()))
                .filter(member -> statusMatches(member.getStatus(), query.getStatus()))
                .sorted(java.util.Comparator.comparing(ShopMemberDto::getMemberId))
                .toList(), query);
    }

    @Override
    public ShopMemberDto updateShopMember(Long tenantId, Long shopId, Long memberId, ShopMemberUpdateParam param) {
        ShopMemberDto member = shopMembers.get(memberId);
        if (member == null || !tenantId.equals(member.getTenantId()) || !shopId.equals(member.getShopId())) {
            throw new IllegalArgumentException("店铺成员不存在");
        }
        member.setRoleCode(normalizeRoleCode(param.getRoleCode()));
        member.setNormalizedRole(normalizedRole(member.getRoleCode()));
        member.setStatus(normalizeStatus(param.getStatus()));
        syncUserRoles(member);
        return member;
    }

    @Override
    public synchronized OrganizationUserDto createUser(Long tenantId, Long shopId, UserCreateParam param) {
        String username = required(param.getUsername(), "用户名");
        if (users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username))) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        String tenantRole = normalizeTenantRole(param.getTenantRole());
        String shopRole = normalizeRoleCode(param.getShopRole());
        String status = normalizeStatus(param.getStatus());
        OrganizationUserDto user = user(nextUserId++, username, param.getDisplayName(), param.getEmail(), status,
                List.of(tenantRole), List.of(shopRole));
        user.setPhone(blankToEmpty(param.getPhone()));
        user.setCreatedAt(LocalDateTime.now());
        users.add(user);

        ShopMemberDto member = member(nextMemberId++, tenantId, shopId, user.getUserId(),
                username, user.getDisplayName(), shopRole);
        member.setStatus(status);
        shopMembers.put(member.getMemberId(), member);
        tenants.stream()
                .filter(tenant -> tenantId.equals(tenant.getTenantId()))
                .findFirst()
                .ifPresent(tenant -> tenant.setMemberCount(tenant.getMemberCount() + 1));
        return user;
    }

    @Override
    public OrganizationUserDto resetUserPassword(Long tenantId, Long shopId, Long userId, UserPasswordResetParam param) {
        required(param.getPassword(), "新密码");
        return users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .filter(user -> shopMembers.values().stream()
                        .anyMatch(member -> tenantId.equals(member.getTenantId())
                                && shopId.equals(member.getShopId())
                                && userId.equals(member.getUserId())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Override
    public synchronized TenantDto createTenant(TenantUpsertParam param) {
        String tenantNo = required(param.getTenantNo(), "租户编号");
        if (tenants.stream().anyMatch(tenant -> tenant.getTenantNo().equalsIgnoreCase(tenantNo))) {
            throw new IllegalArgumentException("租户编号已存在: " + tenantNo);
        }
        TenantDto tenant = new TenantDto();
        tenant.setTenantId(nextTenantId++);
        fillTenant(tenant, param);
        tenant.setShopCount(0L);
        tenant.setMemberCount(0L);
        tenant.setCreatedAt(LocalDateTime.now());
        tenants.add(tenant);
        return tenant;
    }

    @Override
    public TenantDto updateTenant(Long tenantId, TenantUpsertParam param) {
        TenantDto tenant = tenants.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("租户不存在"));
        String tenantNo = required(param.getTenantNo(), "租户编号");
        tenants.stream()
                .filter(item -> !tenantId.equals(item.getTenantId()))
                .filter(item -> item.getTenantNo().equalsIgnoreCase(tenantNo))
                .findAny()
                .ifPresent(item -> {
                    throw new IllegalArgumentException("租户编号已存在: " + tenantNo);
                });
        fillTenant(tenant, param);
        return tenant;
    }

    @Override
    public synchronized ShopDto createShop(Long tenantId, ShopUpsertParam param) {
        String shopNo = required(param.getShopNo(), "店铺编号");
        if (shops.stream().anyMatch(shop -> tenantId.equals(shop.getTenantId()) && shop.getShopNo().equalsIgnoreCase(shopNo))) {
            throw new IllegalArgumentException("店铺编号已存在: " + shopNo);
        }
        ShopDto shop = new ShopDto();
        shop.setShopId(nextShopId++);
        shop.setTenantId(tenantId);
        shop.setMemberCount(0L);
        shop.setCreatedAt(LocalDateTime.now());
        fillShop(shop, param);
        shops.add(shop);
        refreshTenantShopCount(tenantId);
        return shop;
    }

    @Override
    public ShopDto updateShop(Long tenantId, Long shopId, ShopUpsertParam param) {
        ShopDto shop = shops.stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && shopId.equals(item.getShopId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
        String shopNo = required(param.getShopNo(), "店铺编号");
        shops.stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && !shopId.equals(item.getShopId()))
                .filter(item -> item.getShopNo().equalsIgnoreCase(shopNo))
                .findAny()
                .ifPresent(item -> {
                    throw new IllegalArgumentException("店铺编号已存在: " + shopNo);
                });
        fillShop(shop, param);
        return shop;
    }

    @Override
    public synchronized ShopMemberDto addShopMember(Long tenantId, Long shopId, ShopMemberCreateParam param) {
        ShopDto shop = shops.stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && shopId.equals(item.getShopId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
        OrganizationUserDto user = users.stream()
                .filter(item -> param.getUserId().equals(item.getUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        shopMembers.values().stream()
                .filter(member -> shopId.equals(member.getShopId()) && param.getUserId().equals(member.getUserId()))
                .findAny()
                .ifPresent(member -> {
                    throw new IllegalArgumentException("店铺成员已存在");
                });
        String roleCode = normalizeRoleCode(param.getRoleCode());
        String status = normalizeStatus(param.getStatus());
        ShopMemberDto member = member(nextMemberId++, tenantId, shopId, user.getUserId(), user.getUsername(), user.getDisplayName(), roleCode);
        member.setShopName(shop.getShopName());
        member.setStatus(status);
        member.setJoinedAt(LocalDateTime.now());
        shopMembers.put(member.getMemberId(), member);
        shop.setMemberCount(shop.getMemberCount() + 1);
        syncUserRoles(member);
        return member;
    }

    @Override
    public synchronized ShopConfigDto saveShopConfig(Long tenantId, Long shopId, Long userId, ShopConfigUpsertParam param) {
        ensureShop(tenantId, shopId);
        String configKey = required(param.getConfigKey(), "配置键");
        String valueType = normalizeValueType(param.getValueType());
        ShopConfigDto config = shopConfigs.values().stream()
                .filter(item -> tenantId.equals(item.getTenantId())
                        && shopId.equals(item.getShopId())
                        && item.getConfigKey().equals(configKey))
                .findFirst()
                .orElseGet(() -> {
                    ShopConfigDto item = new ShopConfigDto();
                    item.setConfigId(nextConfigId++);
                    item.setTenantId(tenantId);
                    item.setShopId(shopId);
                    item.setConfigKey(configKey);
                    shopConfigs.put(item.getConfigId(), item);
                    return item;
                });
        config.setConfigValue(required(param.getConfigValue(), "配置值"));
        config.setValueType(valueType);
        config.setUpdatedBy(userId);
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private void syncUserRoles(ShopMemberDto member) {
        users.stream()
                .filter(user -> member.getUserId().equals(user.getUserId()))
                .findFirst()
                .ifPresent(user -> user.setShopRoles(List.of(member.getRoleCode())));
    }

    private List<OrganizationUserDto> seedUsers() {
        return new ArrayList<>(List.of(
                user(1L, "admin", "ShopOps 管理员", "admin@shopops.local", "ENABLED",
                        List.of("TENANT_ADMIN"), List.of("SHOP_OWNER")),
                user(2L, "operator", "ShopOps 运营", "operator@shopops.local", "ENABLED",
                        List.of("TENANT_OPERATOR"), List.of("SHOP_OPERATOR")),
                user(3L, "viewer", "ShopOps 观察员", "viewer@shopops.local", "ENABLED",
                        List.of("TENANT_VIEWER"), List.of("SHOP_VIEWER"))
        ));
    }

    private List<TenantDto> seedTenants() {
        TenantDto tenant = new TenantDto();
        tenant.setTenantId(1L);
        tenant.setTenantNo("TENANT_DEMO");
        tenant.setTenantName("演示租户");
        tenant.setStatus("ENABLED");
        tenant.setPlanType("PRO");
        tenant.setContactName("ShopOps Admin");
        tenant.setContactPhone("13800000000");
        tenant.setShopCount(1L);
        tenant.setMemberCount(3L);
        tenant.setCreatedAt(LocalDateTime.now().minusDays(30));
        return new ArrayList<>(List.of(tenant));
    }

    private Map<Long, ShopMemberDto> seedShopMembers() {
        Map<Long, ShopMemberDto> members = new ConcurrentHashMap<>();
        members.put(1L, member(1L, 1L, 1L, 1L, "admin", "ShopOps 管理员", "SHOP_OWNER"));
        members.put(2L, member(2L, 1L, 1L, 2L, "operator", "ShopOps 运营", "SHOP_OPERATOR"));
        members.put(3L, member(3L, 1L, 1L, 3L, "viewer", "ShopOps 观察员", "SHOP_VIEWER"));
        return members;
    }

    private List<ShopDto> seedShops() {
        ShopDto shop = new ShopDto();
        shop.setShopId(1L);
        shop.setTenantId(1L);
        shop.setShopNo("SHOP_DEFAULT");
        shop.setShopName("演示店铺");
        shop.setPlatformType("mock.mall");
        shop.setOwnerId(1L);
        shop.setStatus("ENABLED");
        shop.setMemberCount(3L);
        shop.setCreatedAt(LocalDateTime.now().minusDays(30));
        return new ArrayList<>(List.of(shop));
    }

    private Map<Long, ShopConfigDto> seedShopConfigs() {
        Map<Long, ShopConfigDto> configs = new ConcurrentHashMap<>();
        configs.put(1L, config(1L, "refund_rate_warn_threshold", "0.08", "number"));
        configs.put(2L, config(2L, "negative_comment_warn_threshold", "10", "number"));
        configs.put(3L, config(3L, "agent_tool_approval_enabled", "true", "boolean"));
        return configs;
    }

    private ShopConfigDto config(Long configId, String key, String value, String valueType) {
        ShopConfigDto config = new ShopConfigDto();
        config.setConfigId(configId);
        config.setTenantId(1L);
        config.setShopId(1L);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setValueType(valueType);
        config.setUpdatedBy(1L);
        config.setUpdatedAt(LocalDateTime.now().minusDays(30));
        return config;
    }

    private OrganizationUserDto user(Long userId, String username, String displayName, String email, String status,
                                     List<String> tenantRoles, List<String> shopRoles) {
        OrganizationUserDto user = new OrganizationUserDto();
        user.setUserId(userId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPhone("");
        user.setStatus(status);
        user.setTenantRoles(new ArrayList<>(tenantRoles));
        user.setShopRoles(new ArrayList<>(shopRoles));
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        return user;
    }

    private ShopMemberDto member(Long memberId, Long tenantId, Long shopId, Long userId,
                                 String username, String displayName, String roleCode) {
        ShopMemberDto member = new ShopMemberDto();
        member.setMemberId(memberId);
        member.setTenantId(tenantId);
        member.setShopId(shopId);
        member.setShopName("演示店铺");
        member.setUserId(userId);
        member.setUsername(username);
        member.setDisplayName(displayName);
        member.setRoleCode(roleCode);
        member.setNormalizedRole(normalizedRole(roleCode));
        member.setStatus("ENABLED");
        member.setJoinedAt(LocalDateTime.now().minusDays(30));
        return member;
    }

    private <T> CommonPage<T> page(List<T> source, OrganizationQueryParam query) {
        int from = Math.min(query.offset(), source.size());
        int to = Math.min(from + query.safePageSize(), source.size());
        return CommonPage.of(source.subList(from, to), query.safePageNum(), query.safePageSize(), (long) source.size());
    }

    private boolean matches(String first, String second, String third, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String target = (first + " " + second + " " + third).toLowerCase(Locale.ROOT);
        return target.contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean statusMatches(String status, String expected) {
        return expected == null || expected.isBlank() || status.equalsIgnoreCase(expected);
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
        tenant.setPlanType(blankToEmpty(param.getPlanType()));
        tenant.setContactName(blankToEmpty(param.getContactName()));
        tenant.setContactPhone(blankToEmpty(param.getContactPhone()));
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

    private ShopDto ensureShop(Long tenantId, Long shopId) {
        return shops.stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && shopId.equals(item.getShopId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
    }

    private String normalizeValueType(String valueType) {
        String value = valueType == null ? "" : valueType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("string", "number", "boolean", "json").contains(value)) {
            throw new IllegalArgumentException("不支持的配置类型: " + valueType);
        }
        return value;
    }

    private void refreshTenantShopCount(Long tenantId) {
        tenants.stream()
                .filter(tenant -> tenantId.equals(tenant.getTenantId()))
                .findFirst()
                .ifPresent(tenant -> tenant.setShopCount(shops.stream().filter(shop -> tenantId.equals(shop.getTenantId())).count()));
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizedRole(String roleCode) {
        return switch (roleCode) {
            case "SHOP_OWNER", "SHOP_ADMIN" -> "ADMIN";
            case "SHOP_OPERATOR" -> "OPERATOR";
            case "SHOP_VIEWER" -> "VIEWER";
            default -> "VIEWER";
        };
    }
}
