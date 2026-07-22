package com.sirithree.shopops.admin.connector.service;

import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogQueryParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface ConnectorApiCallLogService {
    ConnectorApiCallLogDto record(ConnectorApiCallLogCreateCommand command);

    CommonPage<ConnectorApiCallLogDto> list(Long tenantId, Long shopId, ConnectorApiCallLogQueryParam param);
}
