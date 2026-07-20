package com.sirithree.shopops.admin.approval.controller;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestCreateParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/approvals")
public class ApprovalController {
    private final ApprovalRequestService approvalRequestService;

    public ApprovalController(ApprovalRequestService approvalRequestService) {
        this.approvalRequestService = approvalRequestService;
    }

    @PostMapping
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<ApprovalRequestDto> create(@Valid @RequestBody ApprovalRequestCreateParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(approvalRequestService.create(
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                context.getUsername(),
                param
        ));
    }

    @GetMapping
    public CommonResult<CommonPage<ApprovalRequestDto>> list(ApprovalRequestQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(approvalRequestService.list(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping("/{approvalId}")
    public CommonResult<ApprovalRequestDto> get(@PathVariable Long approvalId) {
        RequestContext context = RequestContextHolder.current();
        return approvalRequestService.get(context.getTenantId(), context.getShopId(), approvalId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("审批请求不存在"));
    }

    @PostMapping("/{approvalId}/approve")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ApprovalRequestDto> approve(@PathVariable Long approvalId,
                                                    @RequestBody(required = false) ApprovalDecisionParam param) {
        RequestContext context = RequestContextHolder.current();
        return approvalRequestService.approve(
                        context.getTenantId(),
                        context.getShopId(),
                        approvalId,
                        context.getUserId(),
                        context.getUsername(),
                        param
                )
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("审批请求不存在或已处理"));
    }

    @PostMapping("/{approvalId}/reject")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ApprovalRequestDto> reject(@PathVariable Long approvalId,
                                                   @RequestBody(required = false) ApprovalDecisionParam param) {
        RequestContext context = RequestContextHolder.current();
        return approvalRequestService.reject(
                        context.getTenantId(),
                        context.getShopId(),
                        approvalId,
                        context.getUserId(),
                        context.getUsername(),
                        param
                )
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("审批请求不存在或已处理"));
    }

    @PostMapping("/{approvalId}/withdraw")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<ApprovalRequestDto> withdraw(@PathVariable Long approvalId,
                                                     @RequestBody(required = false) ApprovalDecisionParam param) {
        RequestContext context = RequestContextHolder.current();
        return approvalRequestService.withdraw(
                        context.getTenantId(),
                        context.getShopId(),
                        approvalId,
                        context.getUserId(),
                        context.getUsername(),
                        param
                )
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("审批请求不存在或已处理"));
    }
}
