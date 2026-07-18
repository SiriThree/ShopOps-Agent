package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.UserRoleProfile;
import com.sirithree.shopops.admin.auth.service.UserRoleService;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryUserRoleService implements UserRoleService {
    @Override
    public Optional<UserRoleProfile> getUserRoleProfile(Long tenantId, Long shopId, Long userId) {
        return Optional.empty();
    }
}
