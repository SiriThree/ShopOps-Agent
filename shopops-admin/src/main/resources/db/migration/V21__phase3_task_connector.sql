ALTER TABLE agent_task
  ADD COLUMN worker_id varchar(128) DEFAULT NULL AFTER error_message,
  ADD COLUMN locked_at datetime DEFAULT NULL AFTER worker_id,
  ADD COLUMN lease_expire_at datetime DEFAULT NULL AFTER locked_at,
  ADD COLUMN heartbeat_at datetime DEFAULT NULL AFTER lease_expire_at,
  ADD COLUMN attempt int NOT NULL DEFAULT 0 AFTER heartbeat_at,
  ADD COLUMN max_attempts int NOT NULL DEFAULT 3 AFTER attempt,
  ADD COLUMN error_type varchar(64) DEFAULT NULL AFTER max_attempts,
  ADD COLUMN status_reason varchar(500) DEFAULT NULL AFTER error_type,
  ADD COLUMN cancel_requested_at datetime DEFAULT NULL AFTER status_reason,
  ADD KEY idx_agent_task_lease (status, lease_expire_at),
  ADD KEY idx_agent_task_worker (worker_id, status);

ALTER TABLE connector_sync_job
  ADD COLUMN cursor_value varchar(255) DEFAULT NULL AFTER detail_json,
  ADD COLUMN checkpoint_json json DEFAULT NULL AFTER cursor_value,
  ADD COLUMN error_type varchar(64) DEFAULT NULL AFTER checkpoint_json,
  ADD COLUMN next_retry_at datetime DEFAULT NULL AFTER error_type,
  ADD COLUMN worker_id varchar(128) DEFAULT NULL AFTER next_retry_at,
  ADD COLUMN lease_expire_at datetime DEFAULT NULL AFTER worker_id,
  ADD COLUMN heartbeat_at datetime DEFAULT NULL AFTER lease_expire_at,
  ADD COLUMN cancel_requested_at datetime DEFAULT NULL AFTER heartbeat_at,
  ADD KEY idx_connector_sync_lease (status, lease_expire_at),
  ADD KEY idx_connector_sync_retry (status, next_retry_at);

CREATE TABLE connector_sync_item (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  shop_id bigint NOT NULL,
  connector_code varchar(80) NOT NULL,
  external_type varchar(40) NOT NULL,
  external_id varchar(128) NOT NULL,
  external_version varchar(64) DEFAULT NULL,
  payload_hash varchar(64) NOT NULL,
  payload_json json NOT NULL,
  first_seen_at datetime NOT NULL,
  last_seen_at datetime NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_connector_external_item (tenant_id, shop_id, connector_code, external_type, external_id),
  KEY idx_connector_sync_item_scope (tenant_id, shop_id, connector_code, last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Idempotent connector sync staging records';

UPDATE agent_task SET status='PENDING' WHERE status='CREATED';
UPDATE agent_task SET status='SUCCEEDED' WHERE status='SUCCESS';
UPDATE agent_task SET status='NEEDS_MANUAL_ACTION', status_reason='Legacy degraded result requires review' WHERE status='DEGRADED';
