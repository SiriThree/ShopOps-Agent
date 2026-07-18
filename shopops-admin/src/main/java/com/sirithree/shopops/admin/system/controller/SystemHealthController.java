package com.sirithree.shopops.admin.system.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.system.service.SystemHealthService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {
    private final SystemHealthService systemHealthService;

    public SystemHealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/health")
    public CommonResult<Map<String, Object>> health() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(systemHealthService.getHealth(context.getTenantId()));
    }
}
