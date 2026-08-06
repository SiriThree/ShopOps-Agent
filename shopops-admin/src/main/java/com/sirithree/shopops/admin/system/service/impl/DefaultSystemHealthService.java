package com.sirithree.shopops.admin.system.service.impl;

import com.sirithree.shopops.admin.system.service.SystemHealthService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
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
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final ObjectProvider<ConnectionFactory> rabbitConnectionFactoryProvider;
    private final String persistence;

    public DefaultSystemHealthService(McpToolService mcpToolService,
                                      ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                                      ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
                                      ObjectProvider<ConnectionFactory> rabbitConnectionFactoryProvider,
                                      @Value("${shopops.persistence:memory}") String persistence) {
        this.mcpToolService = mcpToolService;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
        this.rabbitConnectionFactoryProvider = rabbitConnectionFactoryProvider;
        this.persistence = persistence;
    }

    @Override
    public Map<String, Object> getHealth(Long tenantId) {
        Map<String, Object> database = checkDatabase();
        Map<String, Object> flyway = checkFlyway();
        Map<String, Object> redis = checkRedis();
        Map<String, Object> rabbitmq = checkRabbitMq();
        Map<String, Object> toolRegistry = checkToolRegistry(tenantId);

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("database", database);
        checks.put("flyway", flyway);
        checks.put("redis", redis);
        checks.put("rabbitmq", rabbitmq);
        checks.put("toolRegistry", toolRegistry);

        boolean up = checks.values().stream()
                .allMatch(check -> "UP".equals(((Map<?, ?>) check).get("status")));

        return Map.of(
                "status", up ? "UP" : "DOWN",
                "persistence", persistence,
                "timestamp", LocalDateTime.now().toString(),
                "checks", checks
        );
    }

    private Map<String, Object> checkDatabase() {
        if (isMemoryMode()) {
            return skipped("Current persistence mode is memory.");
        }
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getObject();
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (Integer.valueOf(1).equals(result)) {
                return Map.of("status", "UP", "mode", "REQUIRED");
            }
            return Map.of("status", "DOWN", "mode", "REQUIRED", "message", "Unexpected SELECT 1 result.");
        } catch (RuntimeException ex) {
            return down(ex);
        }
    }

    private Map<String, Object> checkFlyway() {
        if (isMemoryMode()) {
            return skipped("Current persistence mode is memory.");
        }
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getObject();
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    SELECT version, description, installed_rank
                    FROM flyway_schema_history
                    WHERE success = 1
                    ORDER BY installed_rank DESC
                    LIMIT 1
                    """);
            Map<String, Object> result = requiredUp();
            result.put("version", row.get("version"));
            result.put("description", row.get("description"));
            result.put("installedRank", row.get("installed_rank"));
            return result;
        } catch (RuntimeException ex) {
            return down(ex);
        }
    }

    private Map<String, Object> checkRedis() {
        if (isMemoryMode()) {
            return skipped("Current persistence mode is memory.");
        }
        try {
            RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getObject();
            try (RedisConnection connection = connectionFactory.getConnection()) {
                String pong = connection.ping();
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN");
                result.put("mode", "REQUIRED");
                result.put("ping", pong == null ? "" : pong);
                return result;
            }
        } catch (RuntimeException ex) {
            return down(ex);
        }
    }

    private Map<String, Object> checkRabbitMq() {
        if (isMemoryMode()) {
            return skipped("Current persistence mode is memory.");
        }
        try {
            ConnectionFactory connectionFactory = rabbitConnectionFactoryProvider.getObject();
            try (org.springframework.amqp.rabbit.connection.Connection ignored = connectionFactory.createConnection()) {
                Map<String, Object> result = requiredUp();
                result.put("open", true);
                return result;
            }
        } catch (RuntimeException ex) {
            return down(ex);
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
                "mode", "REQUIRED",
                "requiredCount", REQUIRED_P0_TOOLS.size(),
                "registeredCount", tools.size(),
                "missingTools", missingTools
        );
    }

    private boolean isMemoryMode() {
        return !"jdbc".equalsIgnoreCase(persistence);
    }

    private Map<String, Object> skipped(String message) {
        return Map.of("status", "UP", "mode", "SKIPPED", "message", message);
    }

    private Map<String, Object> down(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return Map.of("status", "DOWN", "mode", "REQUIRED", "message", message);
    }

    private Map<String, Object> requiredUp() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("mode", "REQUIRED");
        return result;
    }
}
