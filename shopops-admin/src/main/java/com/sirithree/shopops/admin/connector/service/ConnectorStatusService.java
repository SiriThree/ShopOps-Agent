package com.sirithree.shopops.admin.connector.service;

import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import java.util.List;

public interface ConnectorStatusService {
    List<ConnectorStatusDto> listStatus(Long tenantId, Long shopId);
}
