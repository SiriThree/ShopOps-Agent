-- ShopOps Agent P0 schema
-- Target: MySQL 5.7+ / 8.x
-- Scope: daily review main flow



CREATE TABLE IF NOT EXISTS `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_no` varchar(64) NOT NULL,
  `tenant_name` varchar(128) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
  `plan_type` varchar(32) DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_no` (`tenant_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant';

CREATE TABLE IF NOT EXISTS `tenant_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role_code` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
  `joined_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant member';

CREATE TABLE IF NOT EXISTS `shop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_no` varchar(64) NOT NULL,
  `shop_name` varchar(128) NOT NULL,
  `platform_type` varchar(32) NOT NULL,
  `owner_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_shop_no` (`tenant_id`, `shop_no`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shop';

CREATE TABLE IF NOT EXISTS `shop_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `config_key` varchar(128) NOT NULL,
  `config_value` text NOT NULL,
  `value_type` varchar(32) NOT NULL DEFAULT 'string',
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_config` (`shop_id`, `config_key`),
  KEY `idx_tenant_shop` (`tenant_id`, `shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shop config';

CREATE TABLE IF NOT EXISTS `shop_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role_code` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
  `joined_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_user` (`shop_id`, `user_id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shop member';

CREATE TABLE IF NOT EXISTS `mcp_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL COMMENT 'NULL means platform global tool',
  `tool_code` varchar(128) NOT NULL,
  `tool_name` varchar(128) NOT NULL,
  `category` varchar(64) NOT NULL,
  `description` text DEFAULT NULL,
  `input_schema` json NOT NULL,
  `output_schema` json NOT NULL,
  `permission_code` varchar(128) NOT NULL,
  `risk_level` varchar(32) NOT NULL,
  `need_approval` tinyint NOT NULL DEFAULT 0,
  `idempotent` tinyint NOT NULL DEFAULT 1,
  `timeout_ms` int NOT NULL DEFAULT 10000,
  `retry_count` int NOT NULL DEFAULT 0,
  `connector_code` varchar(64) DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `version` varchar(32) NOT NULL DEFAULT '1.0.0',
  `owner` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_tenant_code_version` (`tenant_id`, `tool_code`, `version`),
  KEY `idx_tool_code` (`tool_code`),
  KEY `idx_permission_code` (`permission_code`),
  KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP tool registry';

CREATE TABLE IF NOT EXISTS `agent_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `task_no` varchar(64) NOT NULL,
  `task_type` varchar(64) NOT NULL,
  `user_input` text NOT NULL,
  `status` varchar(32) NOT NULL,
  `priority` int NOT NULL DEFAULT 5,
  `plan_json` json DEFAULT NULL,
  `result_summary` text DEFAULT NULL,
  `trace_id` varchar(128) NOT NULL,
  `report_id` bigint DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_tenant_shop` (`tenant_id`, `shop_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent task';

CREATE TABLE IF NOT EXISTS `agent_task_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `step_no` int NOT NULL,
  `step_name` varchar(128) DEFAULT NULL,
  `tool_code` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `depends_on` varchar(256) DEFAULT NULL,
  `input_json` json DEFAULT NULL,
  `output_json` json DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `approval_id` bigint DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_step_no` (`task_id`, `step_no`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent task step';

CREATE TABLE IF NOT EXISTS `agent_task_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) DEFAULT NULL,
  `event_data_json` json DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent task event';

CREATE TABLE IF NOT EXISTS `tool_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `step_id` bigint DEFAULT NULL,
  `trace_id` varchar(128) NOT NULL,
  `span_id` varchar(128) NOT NULL,
  `user_id` bigint NOT NULL,
  `tool_code` varchar(128) NOT NULL,
  `tool_version` varchar(32) DEFAULT NULL,
  `input_json` json DEFAULT NULL,
  `output_json` json DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `risk_level` varchar(32) DEFAULT NULL,
  `approval_id` bigint DEFAULT NULL,
  `latency_ms` int DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_task_step` (`task_id`, `step_id`),
  KEY `idx_shop_tool` (`shop_id`, `tool_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tool call log';

CREATE TABLE IF NOT EXISTS `operation_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `report_no` varchar(64) NOT NULL,
  `report_type` varchar(64) NOT NULL,
  `title` varchar(256) NOT NULL,
  `content_markdown` mediumtext DEFAULT NULL,
  `content_json` json DEFAULT NULL,
  `evidence_json` json DEFAULT NULL,
  `trace_id` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_shop_type` (`shop_id`, `report_type`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation report';

CREATE TABLE IF NOT EXISTS `trace_span` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint DEFAULT NULL,
  `trace_id` varchar(128) NOT NULL,
  `span_id` varchar(128) NOT NULL,
  `parent_span_id` varchar(128) DEFAULT NULL,
  `span_type` varchar(64) NOT NULL,
  `span_name` varchar(128) NOT NULL,
  `ref_type` varchar(64) DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `input_summary` text DEFAULT NULL,
  `output_summary` text DEFAULT NULL,
  `latency_ms` int DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `started_at` datetime NOT NULL,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_parent_span` (`parent_span_id`),
  KEY `idx_ref` (`ref_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trace span';


