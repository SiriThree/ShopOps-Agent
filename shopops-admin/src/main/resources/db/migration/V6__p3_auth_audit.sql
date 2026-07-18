-- ShopOps Agent P3 auth audit

CREATE TABLE IF NOT EXISTS `auth_audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `event_type` varchar(64) NOT NULL,
  `event_status` varchar(32) NOT NULL,
  `auth_type` varchar(32) DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `failure_reason` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_auth_audit_tenant_shop_created` (`tenant_id`, `shop_id`, `created_at`),
  KEY `idx_auth_audit_user_created` (`tenant_id`, `shop_id`, `user_id`, `created_at`),
  KEY `idx_auth_audit_type_status_created` (`tenant_id`, `shop_id`, `event_type`, `event_status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Authentication audit events';
