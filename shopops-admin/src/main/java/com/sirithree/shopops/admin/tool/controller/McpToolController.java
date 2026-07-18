package com.sirithree.shopops.admin.tool.controller;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools")
public class McpToolController {
    private final McpToolService mcpToolService;

    public McpToolController(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @GetMapping
    public CommonResult<List<McpToolDto>> listTools(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId) {
        return CommonResult.success(mcpToolService.listTools(tenantId));
    }

    @GetMapping("/{toolCode}")
    public CommonResult<McpToolDto> getTool(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
                                            @PathVariable String toolCode) {
        McpToolDto tool = mcpToolService.getTool(tenantId, toolCode);
        if (tool == null) {
            return CommonResult.failed("工具不存在");
        }
        return CommonResult.success(tool);
    }
}
