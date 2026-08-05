package com.sirithree.shopops.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.auth.exception.AccessDeniedException;
import com.sirithree.shopops.admin.auth.service.impl.InMemoryAuthorizationService;
import org.junit.jupiter.api.Test;

class InMemoryAuthorizationServiceTest {
    private final InMemoryAuthorizationService service = new InMemoryAuthorizationService();

    @Test
    void shouldRejectUnassignedShopAndTenant() {
        assertThatThrownBy(() -> service.resolve(2L, 1L, 1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.resolve(1L, 2L, 1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldExposePermissionSnapshotForAssignedShop() {
        assertThat(service.resolve(1L, 1L, 1L).permissions()).contains(PermissionCode.AGENT_EXECUTE);
    }
}
