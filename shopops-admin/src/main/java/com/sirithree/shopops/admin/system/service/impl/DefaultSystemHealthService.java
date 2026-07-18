package com.sirithree.shopops.admin.system.service.impl;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.system.service.SystemHealthService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DefaultSystemHealthService implements SystemHealthService {
    private static final List<String> REQUIRED_P0_TOOLS = List.of(
            "order.query_summary",
            "comment.query_negative",
            "product.query_candidates",
            "report.generate_daily_review"
    );

    private final McpToolService mcpToolService;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final String persistence;

    public DefaultSystemHealthService(McpToolService mcpToolService,
                                      ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                                      @Value("${shopops.persistence:memory}") String persistence) {
        this.mcpToolService = mcpToolService;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.persistence = persistence;
    }

    @Override
    public Map<String, Object> getHealth(Long tenantId) {
        Map<String, Object> database = checkDatabase();
        Map<String, Object> toolRegistry = checkToolRegistry(tenantId);
        boolean up = "UP".equals(database.get("status")) && "UP".equals(toolRegistry.get("status"));

        return Map.of(
                "status", up ? "UP" : "DOWN",
                "persistence", persistence,
                "timestamp", LocalDateTime.now().toString(),
                "checks", Map.of(
                        "database", database,
                        "toolRegistry", toolRegistry
                )
        );
    }

    private Map<String, Object> checkDatabase() {
        if (!"jdbc".equalsIgnoreCase(persistence)) {
            return Map.of("status", "UP", "mode", "SKIPPED", "message", "当前为 memory 模式，跳过数据库检查");
        }
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getObject();
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of("status", Integer.valueOf(1).equals(result) ? "UP" : "DOWN", "mode", "REQUIRED");
        } catch (RuntimeException ex) {
            return Map.of("status", "DOWN", "mode", "REQUIRED", "message", ex.getMessage());
        }
    }

    private Map<String, Object> checkToolRegistry(Long tenantId) {
        List<McpToolDto> tools = mcpToolService.listTools(tenantId);
        Set<String> toolCodes = tools.stream().map(McpToolDto::getToolCode).collect(Collectors.toSet());
        List<String> missingTools = REQUIRED_P0_TOOLS.stream()
                .filter(toolCode -> !toolCodes.contains(toolCode))
                .toList();
        return Map.of(
                "status", missingTools.isEmpty() ? "UP" : "DOWN",
                "requiredCount", REQUIRED_P0_TOOLS.size(),
                "registeredCount", tools.size(),
                "missingTools", missingTools
        );
    }
}
