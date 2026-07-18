-- ShopOps Agent P3 auth seed

CREATE TABLE IF NOT EXISTS `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `display_name` varchar(128) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User account';

INSERT INTO `user_account` (`id`, `username`, `display_name`, `phone`, `email`, `status`, `created_at`, `updated_at`)
VALUES
  (1, 'admin', 'ShopOps Admin', NULL, 'admin@shopops.local', 1, NOW(), NOW()),
  (2, 'operator', 'ShopOps Operator', NULL, 'operator@shopops.local', 1, NOW(), NOW()),
  (3, 'viewer', 'ShopOps Viewer', NULL, 'viewer@shopops.local', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `email` = VALUES(`email`),
  `status` = VALUES(`status`),
  `updated_at` = NOW();

INSERT INTO `tenant_member` (`tenant_id`, `user_id`, `role_code`, `status`, `joined_at`)
VALUES
  (1, 1, 'TENANT_ADMIN', 1, NOW()),
  (1, 2, 'TENANT_OPERATOR', 1, NOW()),
  (1, 3, 'TENANT_VIEWER', 1, NOW())
ON DUPLICATE KEY UPDATE
  `role_code` = VALUES(`role_code`),
  `status` = VALUES(`status`);

INSERT INTO `shop_member` (`tenant_id`, `shop_id`, `user_id`, `role_code`, `status`, `joined_at`)
VALUES
  (1, 1, 1, 'SHOP_OWNER', 1, NOW()),
  (1, 1, 2, 'SHOP_OPERATOR', 1, NOW()),
  (1, 1, 3, 'SHOP_VIEWER', 1, NOW())
ON DUPLICATE KEY UPDATE
  `role_code` = VALUES(`role_code`),
  `status` = VALUES(`status`);
