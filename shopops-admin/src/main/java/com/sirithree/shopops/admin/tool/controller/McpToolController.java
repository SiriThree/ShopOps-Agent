package com.sirithree.shopops.admin.tool.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public CommonResult<List<McpToolDto>> listTools() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(mcpToolService.listTools(context.getTenantId()));
    }

    @GetMapping("/{toolCode}")
    public CommonResult<McpToolDto> getTool(@PathVariable String toolCode) {
        RequestContext context = RequestContextHolder.current();
        McpToolDto tool = mcpToolService.getTool(context.getTenantId(), toolCode);
        if (tool == null) {
            return CommonResult.failed("工具不存在");
        }
        return CommonResult.success(tool);
    }
}
