-- Phase 8 first batch: true MCP server registry and read-only comment.query_negative binding.

CREATE TABLE `mcp_server` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint DEFAULT NULL,
  `server_code` varchar(128) NOT NULL,
  `server_name` varchar(128) NOT NULL,
  `transport_type` varchar(32) NOT NULL,
  `endpoint_url` varchar(512) NOT NULL,
  `auth_type` varchar(32) NOT NULL,
  `credential_ref` varchar(128) DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `connect_timeout_ms` int NOT NULL DEFAULT 3000,
  `request_timeout_ms` int NOT NULL DEFAULT 5000,
  `protocol_version` varchar(32) DEFAULT NULL,
  `server_info_json` json DEFAULT NULL,
  `capabilities_json` json DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'REGISTERED',
  `last_connected_at` datetime DEFAULT NULL,
  `last_discovered_at` datetime DEFAULT NULL,
  `last_error_code` varchar(64) DEFAULT NULL,
  `last_error_message` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mcp_server_tenant_code` (`tenant_id`, `server_code`),
  KEY `idx_mcp_server_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Registered external MCP servers';

ALTER TABLE `mcp_tool`
  ADD COLUMN `provider_type` varchar(16) NOT NULL DEFAULT 'LOCAL' AFTER `owner`,
  ADD COLUMN `mcp_server_code` varchar(128) DEFAULT NULL AFTER `provider_type`,
  ADD COLUMN `remote_tool_name` varchar(128) DEFAULT NULL AFTER `mcp_server_code`,
  ADD COLUMN `schema_hash` char(64) DEFAULT NULL AFTER `remote_tool_name`,
  ADD COLUMN `remote_version` varchar(32) DEFAULT NULL AFTER `schema_hash`,
  ADD COLUMN `discovery_status` varchar(32) NOT NULL DEFAULT 'READY' AFTER `remote_version`,
  ADD COLUMN `last_discovered_at` datetime DEFAULT NULL AFTER `discovery_status`;

INSERT INTO `mcp_server` (
  `tenant_id`, `server_code`, `server_name`, `transport_type`, `endpoint_url`,
  `auth_type`, `credential_ref`, `enabled`, `connect_timeout_ms`, `request_timeout_ms`,
  `status`, `created_at`, `updated_at`
) VALUES (
  NULL, 'commerce-default', 'ShopOps Commerce MCP Server', 'STREAMABLE_HTTP',
  'http://shopops-commerce-mcp-server:8090/mcp', 'BEARER',
  'env:SHOPOPS_COMMERCE_MCP_TOKEN', 1, 3000, 5000, 'REGISTERED', NOW(), NOW()
);

UPDATE `mcp_tool`
SET `description` = 'Query low-star and high-risk comments through the independent Commerce MCP server.',
    `input_schema` = CAST('{"$schema":"https://json-schema.org/draft/2020-12/schema","additionalProperties":false,"properties":{"endDate":{"format":"date","type":"string"},"minStar":{"default":3,"maximum":5,"minimum":1,"type":"integer"},"shopId":{"description":"Trusted shop scope injected by ShopOps Tool Gateway","minimum":1,"type":"integer"},"startDate":{"format":"date","type":"string"}},"required":["shopId","startDate","endDate"],"type":"object"}' AS JSON),
    `output_schema` = CAST('{"$schema":"https://json-schema.org/draft/2020-12/schema","additionalProperties":false,"properties":{"categoryStats":{"additionalProperties":{"type":"integer"},"type":"object"},"negativeCount":{"minimum":0,"type":"integer"},"riskComments":{"items":{"type":"object"},"type":"array"},"scope":{"type":"object"}},"required":["negativeCount","riskComments","categoryStats","scope"],"type":"object"}' AS JSON),
    `provider_type` = 'MCP',
    `mcp_server_code` = 'commerce-default',
    `remote_tool_name` = 'comment.query_negative',
    `schema_hash` = 'f5448fec19329d3ccbd75397e888351ab8df725e0dabdee81df89f62454e3873',
    `remote_version` = '1.0.0',
    `discovery_status` = 'READY',
    `last_discovered_at` = NULL,
    `updated_at` = NOW()
WHERE `tool_code` = 'comment.query_negative';
