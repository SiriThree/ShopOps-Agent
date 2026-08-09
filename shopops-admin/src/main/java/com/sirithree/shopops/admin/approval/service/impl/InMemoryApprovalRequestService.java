package com.sirithree.shopops.admin.approval.service.impl;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalBatchDecisionResult;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestCreateParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalStatus;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryApprovalRequestService implements ApprovalRequestService {
    private static final String HIGH_RISK_APPROVAL_CONFIRM_TEXT = "确认通过";

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, ApprovalRequestDto> approvals = new ConcurrentHashMap<>();

    @Override
    public ApprovalRequestDto create(Long tenantId, Long shopId, Long requesterId, String requesterName, ApprovalRequestCreateParam param) {
        Long id = idGenerator.getAndIncrement();
        ApprovalRequestDto dto = new ApprovalRequestDto();
        dto.setApprovalId(id);
        dto.setTenantId(tenantId);
        dto.setShopId(shopId);
        dto.setApprovalNo("APR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + id);
        dto.setSourceType(defaultString(param.getSourceType(), "MANUAL"));
        dto.setSourceId(param.getSourceId());
        dto.setTaskId(param.getTaskId());
        dto.setStepId(param.getStepId());
        dto.setTraceId(param.getTraceId());
        dto.setToolCode(param.getToolCode());
        dto.setRiskLevel(defaultString(param.getRiskLevel(), "MEDIUM").toUpperCase(Locale.ROOT));
        dto.setTitle(defaultString(param.getTitle(), "Approval request"));
        dto.setReason(param.getReason());
        dto.setInputSummary(param.getInputSummary());
        dto.setInputHash(param.getInputHash());
        dto.setBusinessObjectId(param.getBusinessObjectId());
        dto.setStatus(ApprovalStatus.PENDING);
        dto.setRequesterId(requesterId);
        dto.setRequesterName(requesterName);
        dto.setCreatedAt(LocalDateTime.now());
        approvals.put(id, dto);
        return dto;
    }

    @Override
    public CommonPage<ApprovalRequestDto> list(Long tenantId, Long shopId, ApprovalRequestQueryParam param) {
        ApprovalRequestQueryParam query = param == null ? new ApprovalRequestQueryParam() : param;
        List<ApprovalRequestDto> filtered = approvals.values().stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && shopId.equals(item.getShopId()))
                .filter(item -> query.getApprovalId() == null || query.getApprovalId().equals(item.getApprovalId()))
                .filter(item -> blank(query.getApprovalNo()) || query.getApprovalNo().equals(item.getApprovalNo()))
                .filter(item -> blank(query.getStatus()) || query.getStatus().equals(item.getStatus()))
                .filter(item -> blank(query.getSourceType()) || query.getSourceType().equals(item.getSourceType()))
                .filter(item -> query.getTaskId() == null || query.getTaskId().equals(item.getTaskId()))
                .filter(item -> blank(query.getTraceId()) || query.getTraceId().equals(item.getTraceId()))
                .filter(item -> blank(query.getToolCode()) || query.getToolCode().equals(item.getToolCode()))
                .filter(item -> blank(query.getRiskLevel()) || query.getRiskLevel().equalsIgnoreCase(item.getRiskLevel()))
                .filter(item -> query.getRequesterId() == null || query.getRequesterId().equals(item.getRequesterId()))
                .filter(item -> query.getApproverId() == null || query.getApproverId().equals(item.getApproverId()))
                .filter(item -> query.getCreatedStart() == null || !item.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(item -> query.getCreatedEnd() == null || !item.getCreatedAt().isAfter(query.getCreatedEnd()))
                .sorted(Comparator.comparing(ApprovalRequestDto::getApprovalId).reversed())
                .toList();
        List<ApprovalRequestDto> page = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(page, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    @Override
    public Optional<ApprovalRequestDto> get(Long tenantId, Long shopId, Long approvalId) {
        ApprovalRequestDto dto = approvals.get(approvalId);
        if (dto == null || !tenantId.equals(dto.getTenantId()) || !shopId.equals(dto.getShopId())) {
            return Optional.empty();
        }
        return Optional.of(dto);
    }

    @Override
    public Optional<ApprovalRequestDto> approve(Long tenantId, Long shopId, Long approvalId, Long approverId, String approverName, ApprovalDecisionParam param) {
        return decide(tenantId, shopId, approvalId, approverId, approverName, param, ApprovalStatus.APPROVED);
    }

    @Override
    public Optional<ApprovalRequestDto> reject(Long tenantId, Long shopId, Long approvalId, Long approverId, String approverName, ApprovalDecisionParam param) {
        return decide(tenantId, shopId, approvalId, approverId, approverName, param, ApprovalStatus.REJECTED);
    }

    @Override
    public Optional<ApprovalRequestDto> withdraw(Long tenantId, Long shopId, Long approvalId, Long operatorId, String operatorName, ApprovalDecisionParam param) {
        return decide(tenantId, shopId, approvalId, operatorId, operatorName, param, ApprovalStatus.WITHDRAWN);
    }

    @Override
    public ApprovalBatchDecisionResult expireStale(Long tenantId, Long shopId, Long operatorId, String operatorName,
                                                   Integer timeoutMinutes, Integer limit) {
        int safeTimeoutMinutes = safePositive(timeoutMinutes, 60, 0, 10080);
        int safeLimit = safePositive(limit, 50, 1, 500);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(safeTimeoutMinutes);
        List<Long> ids = approvals.values().stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && shopId.equals(item.getShopId()))
                .filter(item -> ApprovalStatus.PENDING.equals(item.getStatus()))
                .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isAfter(cutoff))
                .sorted(Comparator.comparing(ApprovalRequestDto::getApprovalId))
                .limit(safeLimit)
                .map(ApprovalRequestDto::getApprovalId)
                .toList();

        ApprovalBatchDecisionResult result = new ApprovalBatchDecisionResult();
        result.setRequestedCount(ids.size());
        ApprovalDecisionParam decisionParam = new ApprovalDecisionParam();
        decisionParam.setComment("审批超时自动关闭");
        for (Long id : ids) {
            Optional<ApprovalRequestDto> expired = decide(tenantId, shopId, id, operatorId, operatorName, decisionParam, ApprovalStatus.EXPIRED);
            if (expired.isPresent()) {
                result.getSucceeded().add(expired.get());
            } else {
                result.getFailedApprovalIds().add(id);
            }
        }
        result.setSuccessCount(result.getSucceeded().size());
        result.setFailedCount(result.getFailedApprovalIds().size());
        return result;
    }

    @Override
    public boolean markExecuting(Long tenantId, Long shopId, Long approvalId) {
        java.util.concurrent.atomic.AtomicBoolean transitioned = new java.util.concurrent.atomic.AtomicBoolean(false);
        approvals.computeIfPresent(approvalId, (id, dto) -> {
            if (tenantId.equals(dto.getTenantId()) && shopId.equals(dto.getShopId())
                    && ApprovalStatus.APPROVED.equals(dto.getStatus())) {
                dto.setStatus(ApprovalStatus.EXECUTING);
                transitioned.set(true);
            }
            return dto;
        });
        return transitioned.get();
    }

    @Override
    public void markExecuted(Long tenantId, Long shopId, Long approvalId) {
        transitionExecution(tenantId, shopId, approvalId, ApprovalStatus.EXECUTING, ApprovalStatus.EXECUTED, null);
    }

    @Override
    public void markExecutionFailed(Long tenantId, Long shopId, Long approvalId, String message) {
        transitionExecution(tenantId, shopId, approvalId, ApprovalStatus.EXECUTING, ApprovalStatus.EXECUTION_FAILED, message);
    }

    private void transitionExecution(Long tenantId, Long shopId, Long approvalId, String from, String to, String message) {
        approvals.computeIfPresent(approvalId, (id, dto) -> {
            if (tenantId.equals(dto.getTenantId()) && shopId.equals(dto.getShopId()) && from.equals(dto.getStatus())) {
                dto.setStatus(to);
                if (message != null && !message.isBlank()) dto.setDecisionComment(message);
            }
            return dto;
        });
    }

    private Optional<ApprovalRequestDto> decide(Long tenantId, Long shopId, Long approvalId, Long approverId,
                                                String approverName, ApprovalDecisionParam param, String status) {
        Optional<ApprovalRequestDto> existing = get(tenantId, shopId, approvalId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        ApprovalRequestDto dto = existing.get();
        if (!ApprovalStatus.PENDING.equals(dto.getStatus())) {
            return Optional.empty();
        }
        validateHighRiskApprovalConfirmation(dto, param, status);
        dto.setStatus(status);
        dto.setApproverId(approverId);
        dto.setApproverName(approverName);
        dto.setDecisionComment(param == null ? null : param.getComment());
        dto.setDecidedAt(LocalDateTime.now());
        return Optional.of(dto);
    }

    private void validateHighRiskApprovalConfirmation(ApprovalRequestDto dto, ApprovalDecisionParam param, String status) {
        if (!ApprovalStatus.APPROVED.equals(status) || !"HIGH".equalsIgnoreCase(dto.getRiskLevel())) {
            return;
        }
        String confirmText = param == null ? null : param.getConfirmText();
        if (!HIGH_RISK_APPROVAL_CONFIRM_TEXT.equals(confirmText == null ? null : confirmText.trim())) {
            throw new IllegalArgumentException("高风险审批通过前需输入确认语：确认通过");
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
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
