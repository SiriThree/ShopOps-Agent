package com.sirithree.shopops.admin.approval.service.impl;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
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
        dto.setStatus(status);
        dto.setApproverId(approverId);
        dto.setApproverName(approverName);
        dto.setDecisionComment(param == null ? null : param.getComment());
        dto.setDecidedAt(LocalDateTime.now());
        return Optional.of(dto);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
