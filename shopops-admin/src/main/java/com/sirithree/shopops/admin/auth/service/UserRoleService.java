package com.sirithree.shopops.admin.auth.service;

import com.sirithree.shopops.admin.auth.domain.UserRoleProfile;
import java.util.Optional;

public interface UserRoleService {
    Optional<UserRoleProfile> getUserRoleProfile(Long tenantId, Long shopId, Long userId);
}
