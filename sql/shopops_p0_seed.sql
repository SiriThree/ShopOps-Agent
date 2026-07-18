-- ShopOps Agent P0 seed data
-- Assumption: user_id = 1 exists in the auth/user table, or is used as a mock operator.

SET NAMES utf8mb4;

INSERT INTO `tenant` (`id`, `tenant_no`, `tenant_name`, `status`, `plan_type`, `contact_name`, `contact_phone`, `created_at`, `updated_at`)
VALUES (1, 'TENANT_DEFAULT', '默认租户', 1, 'enterprise', '管理员', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `tenant_name` = VALUES(`tenant_name`),
  `updated_at` = NOW();

INSERT INTO `tenant_member` (`tenant_id`, `user_id`, `role_code`, `status`, `joined_at`)
VALUES (1, 1, 'TENANT_ADMIN', 1, NOW())
ON DUPLICATE KEY UPDATE
  `role_code` = VALUES(`role_code`),
  `status` = VALUES(`status`);

INSERT INTO `shop` (`id`, `tenant_id`, `shop_no`, `shop_name`, `platform_type`, `owner_id`, `status`, `created_at`, `updated_at`)
VALUES (1, 1, 'SHOP_DEFAULT', '默认演示店铺', 'mock.mall', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `shop_name` = VALUES(`shop_name`),
  `updated_at` = NOW();

INSERT INTO `shop_member` (`tenant_id`, `shop_id`, `user_id`, `role_code`, `status`, `joined_at`)
VALUES (1, 1, 1, 'SHOP_OWNER', 1, NOW())
ON DUPLICATE KEY UPDATE
  `role_code` = VALUES(`role_code`),
  `status` = VALUES(`status`);

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
(
  NULL,
  'order.query_summary',
  '订单核心指标查询',
  'order',
  '查询指定时间范围内的 GMV、订单数、退款金额、退款率和客单价。',
  CAST('{"type":"object","required":["shopId","startDate","endDate"],"properties":{"shopId":{"type":"integer"},"startDate":{"type":"string","format":"date"},"endDate":{"type":"string","format":"date"}}}' AS JSON),
  CAST('{"type":"object","required":["gmv","orderCount","refundAmount","refundRate","avgOrderAmount"],"properties":{"gmv":{"type":"number"},"orderCount":{"type":"integer"},"refundAmount":{"type":"number"},"refundRate":{"type":"number"},"avgOrderAmount":{"type":"number"},"compareYesterday":{"type":"object"},"compareSevenDayAvg":{"type":"object"}}}' AS JSON),
  'order:read',
  'low',
  0,
  1,
  10000,
  0,
  'mock.mall',
  1,
  '1.0.0',
  'platform',
  NOW(),
  NOW()
),
(
  NULL,
  'comment.query_negative',
  '差评风险查询',
  'comment',
  '查询低星评论和包含高风险关键词的评论。',
  CAST('{"type":"object","required":["shopId","startDate","endDate"],"properties":{"shopId":{"type":"integer"},"startDate":{"type":"string","format":"date"},"endDate":{"type":"string","format":"date"},"minStar":{"type":"integer","default":3}}}' AS JSON),
  CAST('{"type":"object","required":["negativeCount","riskComments","categoryStats"],"properties":{"negativeCount":{"type":"integer"},"riskComments":{"type":"array","items":{"type":"object","properties":{"commentId":{"type":"integer"},"productId":{"type":"integer"},"star":{"type":"integer"},"content":{"type":"string"},"riskKeywords":{"type":"array","items":{"type":"string"}}}},"categoryStats":{"type":"object"}}}' AS JSON),
  'comment:read',
  'low',
  0,
  1,
  10000,
  0,
  'mock.mall',
  1,
  '1.0.0',
  'platform',
  NOW(),
  NOW()
),
(
  NULL,
  'product.query_candidates',
  '待优化商品查询',
  'product',
  '查询可能需要运营优化的商品，例如库存高销量低、差评较多或标题不符合规则。',
  CAST('{"type":"object","required":["shopId","startDate","endDate"],"properties":{"shopId":{"type":"integer"},"startDate":{"type":"string","format":"date"},"endDate":{"type":"string","format":"date"},"limit":{"type":"integer","default":10}}}' AS JSON),
  CAST('{"type":"object","required":["candidateCount","products"],"properties":{"candidateCount":{"type":"integer"},"products":{"type":"array","items":{"type":"object","properties":{"productId":{"type":"integer"},"productName":{"type":"string"},"reason":{"type":"string"},"score":{"type":"number"}}}}}' AS JSON),
  'product:read',
  'low',
  0,
  1,
  10000,
  0,
  'mock.mall',
  1,
  '1.0.0',
  'platform',
  NOW(),
  NOW()
),
(
  NULL,
  'report.generate_daily_review',
  '每日经营复盘报告生成',
  'report',
  '根据订单、评论和商品工具结果生成结构化每日经营复盘报告。',
  CAST('{"type":"object","required":["orderSummary","negativeComments","productCandidates","dateRange"],"properties":{"orderSummary":{"type":"object"},"negativeComments":{"type":"object"},"productCandidates":{"type":"object"},"dateRange":{"type":"object"}}}' AS JSON),
  CAST('{"type":"object","required":["title","markdown","summary","evidence"],"properties":{"title":{"type":"string"},"markdown":{"type":"string"},"summary":{"type":"string"},"evidence":{"type":"object"}}}' AS JSON),
  'report:generate',
  'low',
  0,
  1,
  10000,
  0,
  'internal.report',
  1,
  '1.0.0',
  'platform',
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  `tool_name` = VALUES(`tool_name`),
  `description` = VALUES(`description`),
  `input_schema` = VALUES(`input_schema`),
  `output_schema` = VALUES(`output_schema`),
  `permission_code` = VALUES(`permission_code`),
  `risk_level` = VALUES(`risk_level`),
  `need_approval` = VALUES(`need_approval`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW();

