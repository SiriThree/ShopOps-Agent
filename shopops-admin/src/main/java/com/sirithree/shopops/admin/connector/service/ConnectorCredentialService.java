package com.sirithree.shopops.admin.connector.service;

import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialTestResult;
import java.util.List;

public interface ConnectorCredentialService {
    List<ConnectorCredentialDto> list(Long tenantId, Long shopId);

    ConnectorCredentialDto save(Long tenantId, Long shopId, Long userId, ConnectorCredentialParam param);

    ConnectorCredentialDto disable(Long tenantId, Long shopId, String connectorCode);

    ConnectorCredentialTestResult test(Long tenantId, Long shopId, String connectorCode);
}
