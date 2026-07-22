package com.sirithree.shopops.admin.model.controller;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/model-gateway")
public class AdminModelGatewayController {
    private final ModelGatewayService modelGatewayService;

    public AdminModelGatewayController(ModelGatewayService modelGatewayService) {
        this.modelGatewayService = modelGatewayService;
    }

    @PostMapping("/invoke")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<ModelInvokeResult> invoke(@Valid @RequestBody ModelInvokeParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(modelGatewayService.invoke(
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                context.getUsername(),
                param
        ));
    }

    @GetMapping("/call-logs")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<CommonPage<ModelCallLogDto>> callLogs(ModelCallLogQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(modelGatewayService.listLogs(context.getTenantId(), context.getShopId(), query));
    }
}
