package com.sirithree.shopops.admin.approval.service;

import com.sirithree.shopops.admin.approval.domain.ApprovalBatchDecisionResult;
import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestCreateParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Optional;

public interface ApprovalRequestService {
    ApprovalRequestDto create(Long tenantId, Long shopId, Long requesterId, String requesterName, ApprovalRequestCreateParam param);

    CommonPage<ApprovalRequestDto> list(Long tenantId, Long shopId, ApprovalRequestQueryParam param);

    Optional<ApprovalRequestDto> get(Long tenantId, Long shopId, Long approvalId);

    Optional<ApprovalRequestDto> approve(Long tenantId, Long shopId, Long approvalId, Long approverId, String approverName, ApprovalDecisionParam param);

    Optional<ApprovalRequestDto> reject(Long tenantId, Long shopId, Long approvalId, Long approverId, String approverName, ApprovalDecisionParam param);

    Optional<ApprovalRequestDto> withdraw(Long tenantId, Long shopId, Long approvalId, Long operatorId, String operatorName, ApprovalDecisionParam param);

    ApprovalBatchDecisionResult expireStale(
            Long tenantId, Long shopId, Long operatorId, String operatorName, Integer timeoutMinutes, Integer limit);
}
