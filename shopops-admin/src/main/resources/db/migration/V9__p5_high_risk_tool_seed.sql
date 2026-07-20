INSERT INTO `mcp_tool` (
  `tenant_id`, `tool_code`, `tool_name`, `category`, `description`,
  `input_schema`, `output_schema`, `permission_code`, `risk_level`,
  `need_approval`, `idempotent`, `timeout_ms`, `retry_count`,
  `connector_code`, `enabled`, `version`, `owner`, `created_at`, `updated_at`
) VALUES (
  NULL, 'order.refund_execute', 'High Risk Refund Execution', 'order',
  'Demo high-risk tool that requires manual approval before execution.',
  CAST('{"type":"object","required":["shopId","refundAmount"]}' AS JSON),
  CAST('{"type":"object","required":["refundId","status"]}' AS JSON),
  'order:refund', 'high',
  1, 0, 10000, 0,
  'mock.mall', 1, '1.0.0', 'platform', NOW(), NOW()
) ON DUPLICATE KEY UPDATE
  `tool_name` = VALUES(`tool_name`),
  `description` = VALUES(`description`),
  `risk_level` = VALUES(`risk_level`),
  `need_approval` = VALUES(`need_approval`),
  `updated_at` = NOW();
