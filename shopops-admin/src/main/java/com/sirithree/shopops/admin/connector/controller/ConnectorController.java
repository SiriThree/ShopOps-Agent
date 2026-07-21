package com.sirithree.shopops.admin.connector.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/connectors")
public class ConnectorController {
    private final ConnectorStatusService connectorStatusService;

    public ConnectorController(ConnectorStatusService connectorStatusService) {
        this.connectorStatusService = connectorStatusService;
    }

    @GetMapping("/status")
    public CommonResult<List<ConnectorStatusDto>> listStatus() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(connectorStatusService.listStatus(context.getTenantId(), context.getShopId()));
    }
}
