CREATE TABLE IF NOT EXISTS `connector_credential` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `connector_code` varchar(80) NOT NULL,
  `credential_type` varchar(40) NOT NULL,
  `encrypted_secret` varchar(1200) NOT NULL,
  `secret_preview` varchar(32) NOT NULL,
  `status` varchar(20) NOT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_connector_credential_scope` (`tenant_id`, `shop_id`, `connector_code`),
  KEY `idx_connector_credential_tenant_shop` (`tenant_id`, `shop_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
