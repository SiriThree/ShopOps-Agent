package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.UserRoleProfile;
import com.sirithree.shopops.admin.auth.service.UserRoleService;
import com.sirithree.shopops.admin.persistence.mapper.AuthUserMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcUserRoleService implements UserRoleService {
    private final AuthUserMapper authUserMapper;

    public JdbcUserRoleService(AuthUserMapper authUserMapper) {
        this.authUserMapper = authUserMapper;
    }

    @Override
    public Optional<UserRoleProfile> getUserRoleProfile(Long tenantId, Long shopId, Long userId) {
        String username = authUserMapper.selectUsernameById(userId);
        if (username == null) {
            return Optional.empty();
        }
        List<String> roleCodes = authUserMapper.listActiveRoleCodes(tenantId, shopId, userId);
        return Optional.of(new UserRoleProfile(username, normalizeRoles(roleCodes)));
    }

    private List<String> normalizeRoles(List<String> roleCodes) {
        Set<String> roles = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            String role = normalizeRole(roleCode);
            if (role != null) {
                roles.add(role);
            }
        }
        return List.copyOf(roles);
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return switch (roleCode.trim().toUpperCase()) {
            case "TENANT_ADMIN", "SHOP_OWNER", "SHOP_ADMIN", "ADMIN" -> "ADMIN";
            case "TENANT_OPERATOR", "SHOP_OPERATOR", "OPERATOR" -> "OPERATOR";
            case "TENANT_VIEWER", "SHOP_VIEWER", "VIEWER" -> "VIEWER";
            default -> null;
        };
    }
}
