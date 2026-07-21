package com.sirithree.shopops.admin.approval.service.impl;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalBatchDecisionResult;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestCreateParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalStatus;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.persistence.mapper.ApprovalRequestMapper;
import com.sirithree.shopops.admin.persistence.model.ApprovalRequest;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcApprovalRequestService implements ApprovalRequestService {
    private final ApprovalRequestMapper approvalRequestMapper;

    public JdbcApprovalRequestService(ApprovalRequestMapper approvalRequestMapper) {
        this.approvalRequestMapper = approvalRequestMapper;
    }

    @Override
    public ApprovalRequestDto create(Long tenantId, Long shopId, Long requesterId, String requesterName, ApprovalRequestCreateParam param) {
        LocalDateTime now = LocalDateTime.now();
        ApprovalRequest approval = new ApprovalRequest();
        approval.setTenantId(tenantId);
        approval.setShopId(shopId);
        approval.setApprovalNo("APR" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        approval.setSourceType(defaultString(param.getSourceType(), "MANUAL"));
        approval.setSourceId(param.getSourceId());
        approval.setTaskId(param.getTaskId());
        approval.setStepId(param.getStepId());
        approval.setTraceId(param.getTraceId());
        approval.setToolCode(param.getToolCode());
        approval.setRiskLevel(defaultString(param.getRiskLevel(), "MEDIUM").toUpperCase(Locale.ROOT));
        approval.setTitle(defaultString(param.getTitle(), "Approval request"));
        approval.setReason(param.getReason());
        approval.setInputSummary(param.getInputSummary());
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setRequesterId(requesterId);
        approval.setRequesterName(requesterName);
        approval.setCreatedAt(now);
        approval.setUpdatedAt(now);
        approvalRequestMapper.insert(approval);
        return toDto(approval);
    }

    @Override
    public CommonPage<ApprovalRequestDto> list(Long tenantId, Long shopId, ApprovalRequestQueryParam param) {
        ApprovalRequestQueryParam query = param == null ? new ApprovalRequestQueryParam() : param;
        List<ApprovalRequestDto> list = approvalRequestMapper.listByPage(
                        tenantId,
                        shopId,
                        query,
                        query.offset(),
                        query.safePageSize()
                ).stream()
                .map(this::toDto)
                .toList();
        Long total = approvalRequestMapper.countByPage(tenantId, shopId, query);
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    @Override
    public Optional<ApprovalRequestDto> get(Long tenantId, Long shopId, Long approvalId) {
        ApprovalRequest approval = approvalRequestMapper.selectById(tenantId, shopId, approvalId);
        return approval == null ? Optional.empty() : Optional.of(toDto(approval));
    }

    @Override
    public Optional<ApprovalRequestDto> approve(Long tenantId, Long shopId, Long approvalId, Long approverId,
                                                String approverName, ApprovalDecisionParam param) {
        return decide(tenantId, shopId, approvalId, approverId, approverName, param, ApprovalStatus.APPROVED);
    }

    @Override
    public Optional<ApprovalRequestDto> reject(Long tenantId, Long shopId, Long approvalId, Long approverId,
                                               String approverName, ApprovalDecisionParam param) {
        return decide(tenantId, shopId, approvalId, approverId, approverName, param, ApprovalStatus.REJECTED);
    }

    @Override
    public Optional<ApprovalRequestDto> withdraw(Long tenantId, Long shopId, Long approvalId, Long operatorId,
                                                 String operatorName, ApprovalDecisionParam param) {
        return decide(tenantId, shopId, approvalId, operatorId, operatorName, param, ApprovalStatus.WITHDRAWN);
    }

    @Override
    public ApprovalBatchDecisionResult expireStale(Long tenantId, Long shopId, Long operatorId, String operatorName,
                                                   Integer timeoutMinutes, Integer limit) {
        int safeTimeoutMinutes = safePositive(timeoutMinutes, 60, 0, 10080);
        int safeLimit = safePositive(limit, 50, 1, 500);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(safeTimeoutMinutes);
        List<ApprovalRequest> stale = approvalRequestMapper.listStalePending(tenantId, shopId, cutoff, safeLimit);

        ApprovalBatchDecisionResult result = new ApprovalBatchDecisionResult();
        result.setRequestedCount(stale.size());
        ApprovalDecisionParam decisionParam = new ApprovalDecisionParam();
        decisionParam.setComment("审批超时自动关闭");
        for (ApprovalRequest item : stale) {
            Optional<ApprovalRequestDto> expired = decide(tenantId, shopId, item.getId(), operatorId, operatorName, decisionParam, ApprovalStatus.EXPIRED);
            if (expired.isPresent()) {
                result.getSucceeded().add(expired.get());
            } else {
                result.getFailedApprovalIds().add(item.getId());
            }
        }
        result.setSuccessCount(result.getSucceeded().size());
        result.setFailedCount(result.getFailedApprovalIds().size());
        return result;
    }

    private Optional<ApprovalRequestDto> decide(Long tenantId, Long shopId, Long approvalId, Long approverId,
                                                String approverName, ApprovalDecisionParam param, String status) {
        ApprovalRequest approval = new ApprovalRequest();
        LocalDateTime now = LocalDateTime.now();
        approval.setId(approvalId);
        approval.setTenantId(tenantId);
        approval.setShopId(shopId);
        approval.setStatus(status);
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setDecisionComment(param == null ? null : param.getComment());
        approval.setDecidedAt(now);
        approval.setUpdatedAt(now);
        int updated = approvalRequestMapper.decide(approval);
        if (updated == 0) {
            return Optional.empty();
        }
        return get(tenantId, shopId, approvalId);
    }

    private ApprovalRequestDto toDto(ApprovalRequest approval) {
        ApprovalRequestDto dto = new ApprovalRequestDto();
        dto.setApprovalId(approval.getId());
        dto.setTenantId(approval.getTenantId());
        dto.setShopId(approval.getShopId());
        dto.setApprovalNo(approval.getApprovalNo());
        dto.setSourceType(approval.getSourceType());
        dto.setSourceId(approval.getSourceId());
        dto.setTaskId(approval.getTaskId());
        dto.setStepId(approval.getStepId());
        dto.setTraceId(approval.getTraceId());
        dto.setToolCode(approval.getToolCode());
        dto.setRiskLevel(approval.getRiskLevel());
        dto.setTitle(approval.getTitle());
        dto.setReason(approval.getReason());
        dto.setInputSummary(approval.getInputSummary());
        dto.setStatus(approval.getStatus());
        dto.setRequesterId(approval.getRequesterId());
        dto.setRequesterName(approval.getRequesterName());
        dto.setApproverId(approval.getApproverId());
        dto.setApproverName(approval.getApproverName());
        dto.setDecisionComment(approval.getDecisionComment());
        dto.setCreatedAt(approval.getCreatedAt());
        dto.setDecidedAt(approval.getDecidedAt());
        return dto;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int safePositive(Integer value, int defaultValue, int min, int max) {
        int safe = value == null ? defaultValue : value;
        if (safe < min) {
            return min;
        }
        if (safe > max) {
            return max;
        }
        return safe;
    }
}
