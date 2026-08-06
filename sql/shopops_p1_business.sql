-- ShopOps Agent P1 business schema and seed data
-- Target: MySQL 8.x
-- Scope: replace mock order/comment/product candidate tools with real business data.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `product_no` varchar(64) NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `category_name` varchar(64) DEFAULT NULL,
  `sale_price` decimal(12,2) NOT NULL DEFAULT 0.00,
  `stock` int NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'ON_SHELF',
  `title` varchar(256) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_product_no` (`shop_id`, `product_no`),
  KEY `idx_tenant_shop` (`tenant_id`, `shop_id`),
  KEY `idx_category` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product';

CREATE TABLE IF NOT EXISTS `shop_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `order_status` varchar(32) NOT NULL,
  `pay_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `refund_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `paid_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_shop_paid_at` (`shop_id`, `paid_at`),
  KEY `idx_tenant_shop_status` (`tenant_id`, `shop_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shop order';

CREATE TABLE IF NOT EXISTS `shop_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `quantity` int NOT NULL,
  `sale_price` decimal(12,2) NOT NULL,
  `pay_amount` decimal(12,2) NOT NULL,
  `refund_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_shop_product` (`shop_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shop order item';

CREATE TABLE IF NOT EXISTS `product_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `comment_no` varchar(64) NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `star` int NOT NULL,
  `content` varchar(1000) NOT NULL,
  `sentiment` varchar(32) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_no` (`comment_no`),
  KEY `idx_shop_created_at` (`shop_id`, `created_at`),
  KEY `idx_product_star` (`product_id`, `star`),
  KEY `idx_tenant_shop_star` (`tenant_id`, `shop_id`, `star`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product comment';

INSERT INTO `product` (`id`, `tenant_id`, `shop_id`, `product_no`, `product_name`, `category_name`, `sale_price`, `stock`, `status`, `title`, `created_at`, `updated_at`)
VALUES
  (1001, 1, 1, 'P1001', '轻量保温杯 500ml', '家居日用', 89.00, 1280, 'ON_SHELF', '轻量便携保温杯 500ml 男女学生办公水杯', NOW(), NOW()),
  (1008, 1, 1, 'P1008', '便携收纳箱', '家居收纳', 59.00, 860, 'ON_SHELF', '可折叠便携收纳箱 车载后备箱整理箱', NOW(), NOW()),
  (1016, 1, 1, 'P1016', '运动毛巾', '运动户外', 39.00, 540, 'ON_SHELF', '速干运动毛巾', NOW(), NOW()),
  (1024, 1, 1, 'P1024', '厨房沥水架', '厨房用品', 129.00, 320, 'ON_SHELF', '免打孔厨房沥水架 多层碗碟置物架', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `product_name` = VALUES(`product_name`),
  `category_name` = VALUES(`category_name`),
  `sale_price` = VALUES(`sale_price`),
  `stock` = VALUES(`stock`),
  `status` = VALUES(`status`),
  `title` = VALUES(`title`),
  `updated_at` = NOW();

INSERT INTO `shop_order` (`id`, `tenant_id`, `shop_id`, `order_no`, `user_id`, `order_status`, `pay_amount`, `refund_amount`, `paid_at`, `created_at`, `updated_at`)
VALUES
  (20001, 1, 1, 'SO202607180001', 301, 'FINISHED', 178.00, 0.00, '2026-07-18 09:12:00', '2026-07-18 09:10:00', NOW()),
  (20002, 1, 1, 'SO202607180002', 302, 'SHIPPED', 129.00, 0.00, '2026-07-18 10:35:00', '2026-07-18 10:30:00', NOW()),
  (20003, 1, 1, 'SO202607180003', 303, 'FINISHED', 236.00, 59.00, '2026-07-18 11:20:00', '2026-07-18 11:18:00', NOW()),
  (20004, 1, 1, 'SO202607180004', 304, 'PAID', 39.00, 0.00, '2026-07-18 13:42:00', '2026-07-18 13:39:00', NOW()),
  (20005, 1, 1, 'SO202607180005', 305, 'FINISHED', 258.00, 0.00, '2026-07-18 16:05:00', '2026-07-18 16:01:00', NOW()),
  (20006, 1, 1, 'SO202607170001', 306, 'FINISHED', 158.00, 0.00, '2026-07-17 09:11:00', '2026-07-17 09:10:00', NOW()),
  (20007, 1, 1, 'SO202607170002', 307, 'FINISHED', 118.00, 59.00, '2026-07-17 14:22:00', '2026-07-17 14:20:00', NOW()),
  (20008, 1, 1, 'SO202607160001', 308, 'FINISHED', 267.00, 0.00, '2026-07-16 12:12:00', '2026-07-16 12:10:00', NOW()),
  (20009, 1, 1, 'SO202607150001', 309, 'FINISHED', 129.00, 0.00, '2026-07-15 17:18:00', '2026-07-15 17:16:00', NOW()),
  (20010, 1, 1, 'SO202607140001', 310, 'FINISHED', 217.00, 39.00, '2026-07-14 19:01:00', '2026-07-14 19:00:00', NOW()),
  (20011, 1, 1, 'SO202607130001', 311, 'FINISHED', 89.00, 0.00, '2026-07-13 08:45:00', '2026-07-13 08:43:00', NOW()),
  (20012, 1, 1, 'SO202607120001', 312, 'FINISHED', 348.00, 0.00, '2026-07-12 15:30:00', '2026-07-12 15:28:00', NOW())
ON DUPLICATE KEY UPDATE
  `order_status` = VALUES(`order_status`),
  `pay_amount` = VALUES(`pay_amount`),
  `refund_amount` = VALUES(`refund_amount`),
  `paid_at` = VALUES(`paid_at`),
  `updated_at` = NOW();

INSERT INTO `shop_order_item` (`id`, `tenant_id`, `shop_id`, `order_id`, `product_id`, `product_name`, `quantity`, `sale_price`, `pay_amount`, `refund_amount`, `created_at`)
VALUES
  (30001, 1, 1, 20001, 1001, '轻量保温杯 500ml', 2, 89.00, 178.00, 0.00, NOW()),
  (30002, 1, 1, 20002, 1024, '厨房沥水架', 1, 129.00, 129.00, 0.00, NOW()),
  (30003, 1, 1, 20003, 1008, '便携收纳箱', 4, 59.00, 236.00, 59.00, NOW()),
  (30004, 1, 1, 20004, 1016, '运动毛巾', 1, 39.00, 39.00, 0.00, NOW()),
  (30005, 1, 1, 20005, 1024, '厨房沥水架', 2, 129.00, 258.00, 0.00, NOW())
ON DUPLICATE KEY UPDATE
  `quantity` = VALUES(`quantity`),
  `sale_price` = VALUES(`sale_price`),
  `pay_amount` = VALUES(`pay_amount`),
  `refund_amount` = VALUES(`refund_amount`);

INSERT INTO `product_comment` (`id`, `tenant_id`, `shop_id`, `comment_no`, `order_id`, `product_id`, `user_id`, `star`, `content`, `sentiment`, `created_at`)
VALUES
  (50101, 1, 1, 'C202607180001', 20001, 1001, 301, 2, '物流慢，外包装有破损，送到时杯盖有划痕', 'NEGATIVE', '2026-07-18 18:12:00'),
  (50102, 1, 1, 'C202607180002', 20003, 1008, 303, 1, '商品描述不符，容量比页面写的小，已经申请退款', 'NEGATIVE', '2026-07-18 19:20:00'),
  (50103, 1, 1, 'C202607180003', 20004, 1016, 304, 3, '质量一般，吸水性没有预期好，客服回复比较慢', 'NEGATIVE', '2026-07-18 20:05:00'),
  (50104, 1, 1, 'C202607180004', 20002, 1024, 302, 5, '安装方便，厨房整洁了很多', 'POSITIVE', '2026-07-18 21:10:00'),
  (50105, 1, 1, 'C202607170001', 20007, 1008, 307, 2, '收纳箱边角破损，退换货流程有点慢', 'NEGATIVE', '2026-07-17 17:20:00')
ON DUPLICATE KEY UPDATE
  `star` = VALUES(`star`),
  `content` = VALUES(`content`),
  `sentiment` = VALUES(`sentiment`),
  `created_at` = VALUES(`created_at`);
