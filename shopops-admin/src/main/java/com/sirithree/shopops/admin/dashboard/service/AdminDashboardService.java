package com.sirithree.shopops.admin.dashboard.service;

import com.sirithree.shopops.admin.dashboard.domain.AdminDashboardSummaryDto;

public interface AdminDashboardService {
    AdminDashboardSummaryDto getSummary(Long tenantId, Long shopId);
}
