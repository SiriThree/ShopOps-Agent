package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.DataScope;
import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.auth.exception.AccessDeniedException;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuthorizationService implements AuthorizationService {
    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            PermissionCode.DASHBOARD_READ, PermissionCode.ORDER_READ, PermissionCode.ORDER_EXPORT,
            PermissionCode.PRODUCT_READ, PermissionCode.PRODUCT_UPDATE, PermissionCode.REVIEW_READ,
            PermissionCode.REPORT_GENERATE, PermissionCode.TASK_READ, PermissionCode.TASK_CANCEL,
            PermissionCode.APPROVAL_READ, PermissionCode.APPROVAL_REVIEW, PermissionCode.CONNECTOR_READ,
            PermissionCode.CONNECTOR_MANAGE, PermissionCode.TOOL_READ, PermissionCode.TOOL_EXECUTE,
            PermissionCode.AGENT_EXECUTE, PermissionCode.AUDIT_READ, PermissionCode.USER_MANAGE,
            "comment:read", "ad:read", "report:read", "report:export", "feishu:write",
            "order:refund", "product:write", "ad:write"
    );

    @Override
    public AuthorizationSnapshot resolve(Long tenantId, Long currentShopId, Long userId) {
        if (!Long.valueOf(1L).equals(tenantId) || !Long.valueOf(1L).equals(currentShopId)
                || userId == null || userId <= 0) {
            throw new AccessDeniedException("User is not assigned to requested tenant/shop");
        }
        return new AuthorizationSnapshot(List.of(1L), List.of("ADMIN"), ADMIN_PERMISSIONS, DataScope.ALL_TENANT);
    }

    @Override
    public boolean isAuthorized(Long tenantId, Long shopId, Long userId, String permission) {
        try {
            return resolve(tenantId, shopId, userId).permissions().contains(permission);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
