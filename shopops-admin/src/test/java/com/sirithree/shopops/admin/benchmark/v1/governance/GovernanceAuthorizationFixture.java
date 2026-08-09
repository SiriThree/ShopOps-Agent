package com.sirithree.shopops.admin.benchmark.v1.governance;

import com.sirithree.shopops.admin.auth.domain.DataScope;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Test infrastructure boundary that supplies explicit trusted authorization facts to the real Tool Gateway. */
public class GovernanceAuthorizationFixture implements AuthorizationService {
    private final ConcurrentHashMap<String, AuthorizationSnapshot> snapshots = new ConcurrentHashMap<>();

    public void reset() {
        snapshots.clear();
    }

    public void register(Long tenantId, Long shopId, Long userId,
                         List<Long> accessibleShopIds,
                         List<String> roles,
                         Set<String> permissions,
                         DataScope scope) {
        snapshots.put(key(tenantId, shopId, userId), new AuthorizationSnapshot(
                accessibleShopIds == null ? List.of() : List.copyOf(accessibleShopIds),
                roles == null ? List.of() : List.copyOf(roles),
                permissions == null ? Set.of() : Set.copyOf(permissions),
                scope == null ? DataScope.ASSIGNED_SHOPS : scope));
    }

    @Override
    public AuthorizationSnapshot resolve(Long tenantId, Long currentShopId, Long userId) {
        AuthorizationSnapshot snapshot = snapshots.get(key(tenantId, currentShopId, userId));
        if (snapshot == null) {
            throw new SecurityException("Trusted authorization fixture has no identity snapshot");
        }
        if (!snapshot.accessibleShopIds().contains(currentShopId)) {
            throw new SecurityException("Trusted identity cannot access current shop");
        }
        return snapshot;
    }

    @Override
    public boolean isAuthorized(Long tenantId, Long shopId, Long userId, String permission) {
        try {
            return resolve(tenantId, shopId, userId).permissions().contains(permission);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String key(Long tenantId, Long shopId, Long userId) {
        return String.valueOf(tenantId) + ":" + shopId + ":" + userId;
    }
}
