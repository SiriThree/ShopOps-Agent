-- ShopOps Agent P3 login token seed

ALTER TABLE `user_account`
  ADD COLUMN `password_hash` varchar(128) DEFAULT NULL AFTER `username`;

UPDATE `user_account`
SET `password_hash` = 'sha256:9b4ae7b707678ddc613ee713827367b10324f9105218e0b0a5cd098d31a83132',
    `updated_at` = NOW()
WHERE `username` IN ('admin', 'operator', 'viewer');
