package com.sirithree.shopops.admin.organization.service.impl;

import com.sirithree.shopops.admin.organization.domain.OrganizationOverviewDto;
import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
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
    private final Map<Long, ShopMemberDto> shopMembers = seedShopMembers();

    @Override
    public OrganizationOverviewDto overview(Long tenantId, Long shopId) {
        OrganizationOverviewDto overview = new OrganizationOverviewDto();
        overview.setTenantTotal((long) tenants.size());
        overview.setShopTotal(1L);
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

    private void syncUserRoles(ShopMemberDto member) {
        users.stream()
                .filter(user -> member.getUserId().equals(user.getUserId()))
                .findFirst()
                .ifPresent(user -> user.setShopRoles(List.of(member.getRoleCode())));
    }

    private List<OrganizationUserDto> seedUsers() {
        return List.of(
                user(1L, "admin", "ShopOps 管理员", "admin@shopops.local", "ENABLED",
                        List.of("TENANT_ADMIN"), List.of("SHOP_OWNER")),
                user(2L, "operator", "ShopOps 运营", "operator@shopops.local", "ENABLED",
                        List.of("TENANT_OPERATOR"), List.of("SHOP_OPERATOR")),
                user(3L, "viewer", "ShopOps 观察员", "viewer@shopops.local", "ENABLED",
                        List.of("TENANT_VIEWER"), List.of("SHOP_VIEWER"))
        );
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
        return List.of(tenant);
    }

    private Map<Long, ShopMemberDto> seedShopMembers() {
        Map<Long, ShopMemberDto> members = new ConcurrentHashMap<>();
        members.put(1L, member(1L, 1L, 1L, 1L, "admin", "ShopOps 管理员", "SHOP_OWNER"));
        members.put(2L, member(2L, 1L, 1L, 2L, "operator", "ShopOps 运营", "SHOP_OPERATOR"));
        members.put(3L, member(3L, 1L, 1L, 3L, "viewer", "ShopOps 观察员", "SHOP_VIEWER"));
        return members;
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

    private String normalizedRole(String roleCode) {
        return switch (roleCode) {
            case "SHOP_OWNER", "SHOP_ADMIN" -> "ADMIN";
            case "SHOP_OPERATOR" -> "OPERATOR";
            case "SHOP_VIEWER" -> "VIEWER";
            default -> "VIEWER";
        };
    }
}
