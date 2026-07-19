package com.sirithree.shopops.admin.audit.service;

import com.sirithree.shopops.admin.audit.domain.AdminAuditOverviewDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineEventDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface AdminAuditService {
    AdminAuditOverviewDto getOverview(Long tenantId, Long shopId);

    CommonPage<AdminAuditTimelineEventDto> listTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param);
}
