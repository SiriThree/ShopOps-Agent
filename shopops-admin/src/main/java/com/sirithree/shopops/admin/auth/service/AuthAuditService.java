package com.sirithree.shopops.admin.auth.service;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventQueryParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface AuthAuditService {
    void record(AuthAuditEventCreateCommand command);

    CommonPage<AuthAuditEventDto> listEvents(Long tenantId, Long shopId, AuthAuditEventQueryParam param);
}
