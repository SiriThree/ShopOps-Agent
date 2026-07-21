package com.sirithree.shopops.admin.organization.controller;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.organization.domain.OrganizationOverviewDto;
import com.sirithree.shopops.admin.organization.domain.OrganizationQueryParam;
import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import com.sirithree.shopops.admin.organization.service.OrganizationAdminService;
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
@RequestMapping("/api/admin/organization")
public class OrganizationAdminController {
    private final OrganizationAdminService organizationAdminService;
    private final AuthAuditService authAuditService;

    public OrganizationAdminController(OrganizationAdminService organizationAdminService,
                                       AuthAuditService authAuditService) {
        this.organizationAdminService = organizationAdminService;
        this.authAuditService = authAuditService;
    }

    @GetMapping("/overview")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<OrganizationOverviewDto> overview() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.overview(context.getTenantId(), context.getShopId()));
    }

    @GetMapping("/users")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<CommonPage<OrganizationUserDto>> listUsers(OrganizationQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.listUsers(context.getTenantId(), context.getShopId(), query));
    }

    @GetMapping("/tenants")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<CommonPage<TenantDto>> listTenants(OrganizationQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.listTenants(context.getTenantId(), query));
    }

    @GetMapping("/shop-members")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<CommonPage<ShopMemberDto>> listShopMembers(OrganizationQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.listShopMembers(context.getTenantId(), context.getShopId(), query));
    }

    @PostMapping("/shop-members/{memberId}")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ShopMemberDto> updateShopMember(@PathVariable Long memberId,
                                                        @Valid @RequestBody ShopMemberUpdateParam param) {
        RequestContext context = RequestContextHolder.current();
        ShopMemberDto member = organizationAdminService.updateShopMember(
                context.getTenantId(), context.getShopId(), memberId, param);
        recordMemberEvent(context, member);
        return CommonResult.success(member);
    }

    private void recordMemberEvent(RequestContext context, ShopMemberDto member) {
        AuthAuditEventCreateCommand command = new AuthAuditEventCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setUserId(context.getUserId());
        command.setUsername(context.getUsername());
        command.setEventType("ORG_MEMBER_UPDATED");
        command.setEventStatus("SUCCESS");
        command.setAuthType(context.getAuthType());
        command.setRequestId(context.getRequestId());
        command.setFailureReason("成员 " + member.getUsername() + " 已更新为 " + member.getRoleCode() + " / " + member.getStatus());
        authAuditService.record(command);
    }
}
