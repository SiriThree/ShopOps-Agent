-- ShopOps Agent P0 seed data

INSERT INTO `tenant` (`id`, `tenant_no`, `tenant_name`, `status`, `plan_type`, `contact_name`, `contact_phone`, `created_at`, `updated_at`)
VALUES (1, 'TENANT_DEFAULT', 'Default Tenant', 1, 'enterprise', 'Admin', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE `tenant_name` = VALUES(`tenant_name`), `updated_at` = NOW();

INSERT INTO `tenant_member` (`tenant_id`, `user_id`, `role_code`, `status`, `joined_at`)
VALUES (1, 1, 'TENANT_ADMIN', 1, NOW())
ON DUPLICATE KEY UPDATE `role_code` = VALUES(`role_code`), `status` = VALUES(`status`);

INSERT INTO `shop` (`id`, `tenant_id`, `shop_no`, `shop_name`, `platform_type`, `owner_id`, `status`, `created_at`, `updated_at`)
VALUES (1, 1, 'SHOP_DEFAULT', 'Default Demo Shop', 'mock.mall', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `shop_name` = VALUES(`shop_name`), `updated_at` = NOW();

INSERT INTO `shop_member` (`tenant_id`, `shop_id`, `user_id`, `role_code`, `status`, `joined_at`)
VALUES (1, 1, 1, 'SHOP_OWNER', 1, NOW())
ON DUPLICATE KEY UPDATE `role_code` = VALUES(`role_code`), `status` = VALUES(`status`);

INSERT INTO `shop_config` (`tenant_id`, `shop_id`, `config_key`, `config_value`, `value_type`, `updated_by`, `updated_at`)
VALUES
  (1, 1, 'refund_rate_warn_threshold', '0.08', 'number', 1, NOW()),
  (1, 1, 'negative_comment_warn_threshold', '10', 'number', 1, NOW()),
  (1, 1, 'title_min_length', '12', 'number', 1, NOW()),
  (1, 1, 'title_max_length', '60', 'number', 1, NOW())
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = NOW();

INSERT INTO `mcp_tool` (
  `tenant_id`, `tool_code`, `tool_name`, `category`, `description`,
  `input_schema`, `output_schema`, `permission_code`, `risk_level`,
  `need_approval`, `idempotent`, `timeout_ms`, `retry_count`,
  `connector_code`, `enabled`, `version`, `owner`, `created_at`, `updated_at`
) VALUES
(NULL, 'order.query_summary', 'Order Summary Query', 'order', 'Query GMV, order count, refund amount, refund rate and average order amount.',
 CAST('{"type":"object","required":["shopId","startDate","endDate"]}' AS JSON),
 CAST('{"type":"object","required":["gmv","orderCount","refundAmount","refundRate","avgOrderAmount"]}' AS JSON),
 'order:read', 'low', 0, 1, 10000, 0, 'mock.mall', 1, '1.0.0', 'platform', NOW(), NOW()),
(NULL, 'comment.query_negative', 'Negative Comment Query', 'comment', 'Query low-star and high-risk comments.',
 CAST('{"type":"object","required":["shopId","startDate","endDate"]}' AS JSON),
 CAST('{"type":"object","required":["negativeCount","riskComments","categoryStats"]}' AS JSON),
 'comment:read', 'low', 0, 1, 10000, 0, 'mock.mall', 1, '1.0.0', 'platform', NOW(), NOW()),
(NULL, 'product.query_candidates', 'Product Candidate Query', 'product', 'Query products that may need operational optimization.',
 CAST('{"type":"object","required":["shopId","startDate","endDate"]}' AS JSON),
 CAST('{"type":"object","required":["candidateCount","products"]}' AS JSON),
 'product:read', 'low', 0, 1, 10000, 0, 'mock.mall', 1, '1.0.0', 'platform', NOW(), NOW()),
(NULL, 'report.generate_daily_review', 'Daily Review Report Generator', 'report', 'Generate a structured daily review report from tool evidence.',
 CAST('{"type":"object","required":["orderSummary","negativeComments","productCandidates","dateRange"]}' AS JSON),
 CAST('{"type":"object","required":["title","markdown","summary","evidence"]}' AS JSON),
 'report:generate', 'low', 0, 1, 10000, 0, 'internal.report', 1, '1.0.0', 'platform', NOW(), NOW());
