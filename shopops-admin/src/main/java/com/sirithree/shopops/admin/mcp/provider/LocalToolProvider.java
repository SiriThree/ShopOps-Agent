package com.sirithree.shopops.admin.mcp.provider;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import com.sirithree.shopops.admin.tool.service.ToolProvider;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LocalToolProvider implements ToolProvider {
    private final Map<String, ToolExecutor> executors;

    public LocalToolProvider(List<ToolExecutor> executors) {
        this.executors = executors.stream()
                .collect(Collectors.toUnmodifiableMap(ToolExecutor::toolCode, Function.identity()));
    }

    @Override
    public boolean supports(McpToolDto tool) {
        return tool.getProviderType() == null
                || CommerceMcpContracts.PROVIDER_LOCAL.equalsIgnoreCase(tool.getProviderType());
    }

    @Override
    public ToolInvokeResult invoke(ToolInvokeContext context, McpToolDto tool, Object input) {
        ToolExecutor executor = executors.get(tool.getToolCode());
        if (executor == null) {
            return ToolInvokeResult.failed("EXECUTOR_NOT_FOUND",
                    "未注册本地工具执行器: " + tool.getToolCode(), null);
        }
        return executor.execute(context, input);
    }
}
