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
import com.sirithree.shopops.admin.organization.domain.ShopConfigDto;
import com.sirithree.shopops.admin.organization.domain.ShopConfigUpsertParam;
import com.sirithree.shopops.admin.organization.domain.ShopDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberCreateParam;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberUpdateParam;
import com.sirithree.shopops.admin.organization.domain.ShopUpsertParam;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import com.sirithree.shopops.admin.organization.domain.TenantUpsertParam;
import com.sirithree.shopops.admin.organization.domain.UserCreateParam;
import com.sirithree.shopops.admin.organization.domain.UserPasswordResetParam;
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

    @GetMapping("/shops")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<CommonPage<ShopDto>> listShops(OrganizationQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.listShops(context.getTenantId(), query));
    }

    @GetMapping("/shops/{shopId}/configs")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<CommonPage<ShopConfigDto>> listShopConfigs(@PathVariable Long shopId,
                                                                   OrganizationQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.listShopConfigs(context.getTenantId(), shopId, query));
    }

    @GetMapping("/shop-members")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<CommonPage<ShopMemberDto>> listShopMembers(OrganizationQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(organizationAdminService.listShopMembers(context.getTenantId(), context.getShopId(), query));
    }

    @PostMapping("/users")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<OrganizationUserDto> createUser(@Valid @RequestBody UserCreateParam param) {
        RequestContext context = RequestContextHolder.current();
        OrganizationUserDto user = organizationAdminService.createUser(context.getTenantId(), context.getShopId(), param);
        recordOrgEvent(context, "ORG_USER_CREATED", "用户 " + user.getUsername() + " 已创建");
        return CommonResult.success(user);
    }

    @PostMapping("/users/{userId}/password")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<OrganizationUserDto> resetUserPassword(@PathVariable Long userId,
                                                               @Valid @RequestBody UserPasswordResetParam param) {
        RequestContext context = RequestContextHolder.current();
        OrganizationUserDto user = organizationAdminService.resetUserPassword(context.getTenantId(), context.getShopId(), userId, param);
        recordOrgEvent(context, "ORG_USER_PASSWORD_RESET", "用户 " + user.getUsername() + " 已重置密码");
        return CommonResult.success(user);
    }

    @PostMapping("/tenants")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<TenantDto> createTenant(@Valid @RequestBody TenantUpsertParam param) {
        RequestContext context = RequestContextHolder.current();
        TenantDto tenant = organizationAdminService.createTenant(param);
        recordOrgEvent(context, "ORG_TENANT_CREATED", "租户 " + tenant.getTenantNo() + " 已创建");
        return CommonResult.success(tenant);
    }

    @PostMapping("/tenants/{tenantId}")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<TenantDto> updateTenant(@PathVariable Long tenantId,
                                                @Valid @RequestBody TenantUpsertParam param) {
        RequestContext context = RequestContextHolder.current();
        TenantDto tenant = organizationAdminService.updateTenant(tenantId, param);
        recordOrgEvent(context, "ORG_TENANT_UPDATED", "租户 " + tenant.getTenantNo() + " 已更新");
        return CommonResult.success(tenant);
    }

    @PostMapping("/shops")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ShopDto> createShop(@Valid @RequestBody ShopUpsertParam param) {
        RequestContext context = RequestContextHolder.current();
        ShopDto shop = organizationAdminService.createShop(context.getTenantId(), param);
        recordOrgEvent(context, "ORG_SHOP_CREATED", "店铺 " + shop.getShopNo() + " 已创建");
        return CommonResult.success(shop);
    }

    @PostMapping("/shops/{shopId}")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ShopDto> updateShop(@PathVariable Long shopId,
                                            @Valid @RequestBody ShopUpsertParam param) {
        RequestContext context = RequestContextHolder.current();
        ShopDto shop = organizationAdminService.updateShop(context.getTenantId(), shopId, param);
        recordOrgEvent(context, "ORG_SHOP_UPDATED", "店铺 " + shop.getShopNo() + " 已更新");
        return CommonResult.success(shop);
    }

    @PostMapping("/shops/{shopId}/members")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ShopMemberDto> addShopMember(@PathVariable Long shopId,
                                                     @Valid @RequestBody ShopMemberCreateParam param) {
        RequestContext context = RequestContextHolder.current();
        ShopMemberDto member = organizationAdminService.addShopMember(context.getTenantId(), shopId, param);
        recordOrgEvent(context, "ORG_SHOP_MEMBER_ADDED", "店铺成员 " + member.getUsername() + " 已绑定");
        return CommonResult.success(member);
    }

    @PostMapping("/shops/{shopId}/configs")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ShopConfigDto> saveShopConfig(@PathVariable Long shopId,
                                                      @Valid @RequestBody ShopConfigUpsertParam param) {
        RequestContext context = RequestContextHolder.current();
        ShopConfigDto config = organizationAdminService.saveShopConfig(context.getTenantId(), shopId, context.getUserId(), param);
        recordOrgEvent(context, "ORG_SHOP_CONFIG_SAVED", "店铺配置 " + config.getConfigKey() + " 已保存");
        return CommonResult.success(config);
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
        recordOrgEvent(context, "ORG_MEMBER_UPDATED",
                "成员 " + member.getUsername() + " 已更新为 " + member.getRoleCode() + " / " + member.getStatus());
    }

    private void recordOrgEvent(RequestContext context, String eventType, String message) {
        AuthAuditEventCreateCommand command = new AuthAuditEventCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setUserId(context.getUserId());
        command.setUsername(context.getUsername());
        command.setEventType(eventType);
        command.setEventStatus("SUCCESS");
        command.setAuthType(context.getAuthType());
        command.setRequestId(context.getRequestId());
        command.setFailureReason(message);
        authAuditService.record(command);
    }
}
