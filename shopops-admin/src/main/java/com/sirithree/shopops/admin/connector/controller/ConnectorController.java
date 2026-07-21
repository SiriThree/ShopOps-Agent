package com.sirithree.shopops.admin.connector.controller;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialTestResult;
import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.service.ConnectorCredentialService;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/connectors")
public class ConnectorController {
    private final ConnectorStatusService connectorStatusService;
    private final ConnectorCredentialService connectorCredentialService;

    public ConnectorController(ConnectorStatusService connectorStatusService,
                               ConnectorCredentialService connectorCredentialService) {
        this.connectorStatusService = connectorStatusService;
        this.connectorCredentialService = connectorCredentialService;
    }

    @GetMapping("/status")
    public CommonResult<List<ConnectorStatusDto>> listStatus() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorStatusService.listStatus(context.getTenantId(), context.getShopId()));
    }

    @GetMapping("/credentials")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<List<ConnectorCredentialDto>> listCredentials() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorCredentialService.list(context.getTenantId(), context.getShopId()));
    }

    @PostMapping("/credentials")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorCredentialDto> saveCredential(@Valid @RequestBody ConnectorCredentialParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorCredentialService.save(
                context.getTenantId(), context.getShopId(), context.getUserId(), param));
    }

    @PostMapping("/credentials/{connectorCode}/disable")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorCredentialDto> disableCredential(@PathVariable String connectorCode) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorCredentialService.disable(context.getTenantId(), context.getShopId(), connectorCode));
    }

    @PostMapping("/credentials/{connectorCode}/test")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<ConnectorCredentialTestResult> testCredential(@PathVariable String connectorCode) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorCredentialService.test(context.getTenantId(), context.getShopId(), connectorCode));
    }
}
