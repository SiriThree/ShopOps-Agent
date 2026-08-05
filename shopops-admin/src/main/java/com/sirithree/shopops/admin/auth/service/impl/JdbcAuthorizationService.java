package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.DataScope;
import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.auth.service.UserRoleService;
import com.sirithree.shopops.admin.persistence.mapper.AuthUserMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcAuthorizationService implements AuthorizationService {
    private final AuthUserMapper authUserMapper;
    private final UserRoleService userRoleService;

    public JdbcAuthorizationService(AuthUserMapper authUserMapper, UserRoleService userRoleService) {
        this.authUserMapper = authUserMapper;
        this.userRoleService = userRoleService;
    }

    @Override
    public AuthorizationSnapshot resolve(Long tenantId, Long currentShopId, Long userId) {
        List<Long> shopIds = authUserMapper.listAccessibleShopIds(tenantId, userId);
        if (currentShopId == null || !shopIds.contains(currentShopId)) {
            throw new com.sirithree.shopops.admin.auth.exception.AccessDeniedException("Current shop is not assigned to user");
        }
        List<String> roles = userRoleService.getUserRoleProfile(tenantId, currentShopId, userId)
                .map(profile -> profile.getRoles()).orElse(List.of());
        if (roles.isEmpty()) {
            throw new com.sirithree.shopops.admin.auth.exception.AccessDeniedException("User has no active role in current tenant/shop");
        }
        return new AuthorizationSnapshot(shopIds, roles, permissionsFor(roles), dataScopeFor(roles));
    }

    @Override
    public boolean isAuthorized(Long tenantId, Long shopId, Long userId, String permission) {
        try {
            return resolve(tenantId, shopId, userId).permissions().contains(permission);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Set<String> permissionsFor(List<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roles.contains("VIEWER") || roles.contains("OPERATOR") || roles.contains("ADMIN")) {
            permissions.addAll(Set.of(PermissionCode.DASHBOARD_READ, PermissionCode.ORDER_READ,
                    PermissionCode.PRODUCT_READ, PermissionCode.REVIEW_READ, PermissionCode.TASK_READ,
                    PermissionCode.APPROVAL_READ, PermissionCode.CONNECTOR_READ, PermissionCode.TOOL_READ,
                    "comment:read", "ad:read", "report:read"));
        }
        if (roles.contains("OPERATOR") || roles.contains("ADMIN")) {
            permissions.addAll(Set.of(PermissionCode.ORDER_EXPORT, PermissionCode.PRODUCT_UPDATE,
                    PermissionCode.REPORT_GENERATE, PermissionCode.TASK_CANCEL,
                    PermissionCode.TOOL_EXECUTE, PermissionCode.AGENT_EXECUTE,
                    "order:refund", "product:write", "ad:write", "report:export", "feishu:write"));
        }
        if (roles.contains("ADMIN")) {
            permissions.addAll(Set.of(PermissionCode.APPROVAL_REVIEW, PermissionCode.CONNECTOR_MANAGE,
                    PermissionCode.AUDIT_READ, PermissionCode.USER_MANAGE));
        }
        return Set.copyOf(permissions);
    }

    private DataScope dataScopeFor(List<String> roles) {
        return roles.contains("ADMIN") ? DataScope.ALL_TENANT : DataScope.ASSIGNED_SHOPS;
    }
}
