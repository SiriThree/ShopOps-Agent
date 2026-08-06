package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.persistence.mapper.McpToolMapper;
import com.sirithree.shopops.admin.persistence.model.McpTool;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcMcpToolService implements McpToolService {
    private final McpToolMapper mcpToolMapper;

    public JdbcMcpToolService(McpToolMapper mcpToolMapper) {
        this.mcpToolMapper = mcpToolMapper;
    }

    @Override
    public List<McpToolDto> listTools(Long tenantId) {
        return mcpToolMapper.listEnabled(tenantId).stream().map(this::toDto).toList();
    }

    @Override
    public McpToolDto getTool(Long tenantId, String toolCode) {
        McpTool tool = mcpToolMapper.selectEnabledByCode(tenantId, toolCode);
        return tool == null ? null : toDto(tool);
    }

    private McpToolDto toDto(McpTool tool) {
        McpToolDto dto = new McpToolDto(
                tool.getToolCode(),
                tool.getToolName(),
                tool.getCategory(),
                tool.getPermissionCode(),
                tool.getRiskLevel()
        );
        dto.setDescription(tool.getDescription());
        dto.setInputSchema(tool.getInputSchema());
        dto.setOutputSchema(tool.getOutputSchema());
        dto.setNeedApproval(Integer.valueOf(1).equals(tool.getNeedApproval()));
        dto.setIdempotent(Integer.valueOf(1).equals(tool.getIdempotent()));
        dto.setTimeoutMs(tool.getTimeoutMs());
        dto.setRetryCount(tool.getRetryCount());
        dto.setEnabled(Integer.valueOf(1).equals(tool.getEnabled()));
        dto.setVersion(tool.getVersion());
        dto.setProviderType(tool.getProviderType());
        dto.setMcpServerCode(tool.getMcpServerCode());
        dto.setRemoteToolName(tool.getRemoteToolName());
        dto.setSchemaHash(tool.getSchemaHash());
        dto.setRemoteVersion(tool.getRemoteVersion());
        dto.setDiscoveryStatus(tool.getDiscoveryStatus());
        return dto;
    }
}
