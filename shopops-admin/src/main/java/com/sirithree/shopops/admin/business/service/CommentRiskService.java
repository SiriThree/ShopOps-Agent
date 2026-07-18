package com.sirithree.shopops.admin.business.service;

import java.time.LocalDate;
import java.util.Map;

public interface CommentRiskService {
    Map<String, Object> queryNegativeComments(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer minStar);
}
