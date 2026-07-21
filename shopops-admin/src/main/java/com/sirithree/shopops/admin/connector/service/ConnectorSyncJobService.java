package com.sirithree.shopops.admin.connector.service;

import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobCreateParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobQueryParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface ConnectorSyncJobService {
    ConnectorSyncJobDto createAndRun(Long tenantId, Long shopId, Long userId, String requestId, ConnectorSyncJobCreateParam param);

    ConnectorSyncJobDto retry(Long tenantId, Long shopId, Long userId, String requestId, Long jobId);

    CommonPage<ConnectorSyncJobDto> list(Long tenantId, Long shopId, ConnectorSyncJobQueryParam param);
}
