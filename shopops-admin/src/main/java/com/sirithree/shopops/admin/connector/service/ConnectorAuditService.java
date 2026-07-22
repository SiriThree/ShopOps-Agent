package com.sirithree.shopops.admin.connector.service;

import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventQueryParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface ConnectorAuditService {
    void record(ConnectorAuditEventCreateCommand command);

    CommonPage<ConnectorAuditEventDto> listEvents(Long tenantId, Long shopId, ConnectorAuditEventQueryParam param);
}
