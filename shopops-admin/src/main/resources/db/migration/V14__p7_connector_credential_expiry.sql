ALTER TABLE `connector_credential`
  ADD COLUMN `expires_at` datetime DEFAULT NULL AFTER `status`,
  ADD KEY `idx_connector_credential_expires` (`tenant_id`, `shop_id`, `expires_at`);
