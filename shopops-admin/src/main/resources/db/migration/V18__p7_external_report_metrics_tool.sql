INSERT INTO `mcp_tool` (
  `tenant_id`, `tool_code`, `tool_name`, `category`, `description`,
  `input_schema`, `output_schema`, `permission_code`, `risk_level`,
  `need_approval`, `idempotent`, `timeout_ms`, `retry_count`,
  `connector_code`, `enabled`, `version`, `owner`, `created_at`, `updated_at`
) VALUES (
  NULL, 'report.query_external_metrics', 'External Report Metrics Query', 'report', 'Query platform report metrics such as visitors, conversion and channel performance.',
  CAST('{"type":"object","required":["shopId","startDate","endDate"],"properties":{"shopId":{"type":"integer"},"startDate":{"type":"string","format":"date"},"endDate":{"type":"string","format":"date"}}}' AS JSON),
  CAST('{"type":"object","required":["visitorCount","conversionRate","repeatPurchaseRate"],"properties":{"visitorCount":{"type":"integer"},"newVisitorCount":{"type":"integer"},"conversionRate":{"type":"number"},"repeatPurchaseRate":{"type":"number"},"favoriteCount":{"type":"integer"},"cartAddCount":{"type":"integer"},"topChannels":{"type":"array"}}}' AS JSON),
  'report:read', 'low', 0, 1, 10000, 0,
  'file.external-reports', 1, '1.0.0', 'platform', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
  `tool_name` = VALUES(`tool_name`),
  `description` = VALUES(`description`),
  `input_schema` = VALUES(`input_schema`),
  `output_schema` = VALUES(`output_schema`),
  `permission_code` = VALUES(`permission_code`),
  `connector_code` = VALUES(`connector_code`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW();
