package com.sirithree.shopops.admin.approval.controller;

import com.sirithree.shopops.admin.approval.domain.ApprovalBatchDecisionParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalBatchDecisionResult;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/batch/approve")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ApprovalBatchDecisionResult> batchApprove(@Valid @RequestBody ApprovalBatchDecisionParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(batchDecision(context, param, true));
    }

    @PostMapping("/batch/reject")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ApprovalBatchDecisionResult> batchReject(@Valid @RequestBody ApprovalBatchDecisionParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(batchDecision(context, param, false));
    }

    @PostMapping("/expire-stale")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ApprovalBatchDecisionResult> expireStale(@RequestParam(required = false) Integer timeoutMinutes,
                                                                 @RequestParam(required = false) Integer limit) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(approvalRequestService.expireStale(
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                context.getUsername(),
                timeoutMinutes,
                limit
        ));
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

    private ApprovalBatchDecisionResult batchDecision(RequestContext context, ApprovalBatchDecisionParam param, boolean approve) {
        ApprovalBatchDecisionResult result = new ApprovalBatchDecisionResult();
        result.setRequestedCount(param.getApprovalIds().size());
        ApprovalDecisionParam decisionParam = new ApprovalDecisionParam();
        decisionParam.setComment(param.getComment());
        for (Long approvalId : param.getApprovalIds()) {
            var decided = approve
                    ? approvalRequestService.approve(context.getTenantId(), context.getShopId(), approvalId,
                    context.getUserId(), context.getUsername(), decisionParam)
                    : approvalRequestService.reject(context.getTenantId(), context.getShopId(), approvalId,
                    context.getUserId(), context.getUsername(), decisionParam);
            if (decided.isPresent()) {
                result.getSucceeded().add(decided.get());
            } else {
                result.getFailedApprovalIds().add(approvalId);
            }
        }
        result.setSuccessCount(result.getSucceeded().size());
        result.setFailedCount(result.getFailedApprovalIds().size());
        return result;
    }
}
