CREATE TABLE IF NOT EXISTS `connector_audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `connector_code` varchar(80) NOT NULL,
  `event_type` varchar(40) NOT NULL,
  `event_status` varchar(20) NOT NULL,
  `request_id` varchar(80) DEFAULT NULL,
  `message` varchar(500) DEFAULT NULL,
  `detail_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_connector_audit_scope` (`tenant_id`, `shop_id`, `created_at`),
  KEY `idx_connector_audit_connector` (`tenant_id`, `shop_id`, `connector_code`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
