package com.sirithree.shopops.admin.audit.service;

import com.sirithree.shopops.admin.audit.domain.AdminAuditExportDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditOverviewDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditRiskSummaryDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineDetailDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineEventDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Optional;

public interface AdminAuditService {
    AdminAuditOverviewDto getOverview(Long tenantId, Long shopId);

    CommonPage<AdminAuditTimelineEventDto> listTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param);

    Optional<AdminAuditTimelineDetailDto> getTimelineDetail(Long tenantId, Long shopId, String source, String resourceId);

    AdminAuditRiskSummaryDto getRiskSummary(Long tenantId, Long shopId);

    AdminAuditExportDto exportTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param);
}
