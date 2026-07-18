package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import java.util.List;

public interface McpToolService {
    List<McpToolDto> listTools(Long tenantId);

    McpToolDto getTool(Long tenantId, String toolCode);
}
