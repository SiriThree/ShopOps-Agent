package com.sirithree.shopops.admin.auth.service;

import com.sirithree.shopops.admin.auth.domain.DataScope;
import java.util.List;
import java.util.Set;

public interface AuthorizationService {
    AuthorizationSnapshot resolve(Long tenantId, Long currentShopId, Long userId);
    boolean isAuthorized(Long tenantId, Long shopId, Long userId, String permission);

    record AuthorizationSnapshot(List<Long> accessibleShopIds,
                                 List<String> roles,
                                 Set<String> permissions,
                                 DataScope dataScope) {
    }
}
