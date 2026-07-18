package com.sirithree.shopops.admin.tool.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools")
public class ToolInvokeController {
    private final ToolGatewayService toolGatewayService;
    private final ToolCallLogService toolCallLogService;

    public ToolInvokeController(ToolGatewayService toolGatewayService, ToolCallLogService toolCallLogService) {
        this.toolGatewayService = toolGatewayService;
        this.toolCallLogService = toolCallLogService;
    }

    @PostMapping("/{toolCode}/invoke")
    public CommonResult<ToolInvokeResult> invoke(@PathVariable String toolCode,
                                                 @RequestBody Map<String, Object> input) {
        RequestContext requestContext = RequestContextHolder.current();
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(requestContext.getTenantId());
        context.setShopId(requestContext.getShopId());
        context.setUserId(requestContext.getUserId());
        context.setTraceId("tr_manual_" + UUID.randomUUID().toString().replace("-", ""));
        context.setManualInvoke(true);
        return CommonResult.success(toolGatewayService.invoke(context, toolCode, input));
    }

    @GetMapping("/call-logs")
    public CommonResult<?> listCallLogs(@RequestParam Long taskId) {
        return CommonResult.success(toolCallLogService.listByTaskId(taskId));
    }
}
