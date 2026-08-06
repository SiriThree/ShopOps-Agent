-- ShopOps Agent P3 token session lifecycle

CREATE TABLE IF NOT EXISTS `auth_token_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_id` varchar(64) NOT NULL,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `username` varchar(64) NOT NULL,
  `roles_json` varchar(512) NOT NULL,
  `status` varchar(32) NOT NULL,
  `issued_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `last_seen_at` datetime DEFAULT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_token_session_token` (`token_id`),
  KEY `idx_auth_token_session_user_status` (`tenant_id`, `shop_id`, `user_id`, `status`),
  KEY `idx_auth_token_session_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Authentication token sessions';
