ALTER TABLE approval_request
  ADD COLUMN input_hash varchar(64) DEFAULT NULL AFTER input_summary,
  ADD COLUMN business_object_id varchar(128) DEFAULT NULL AFTER tool_code,
  ADD COLUMN execution_started_at datetime DEFAULT NULL AFTER decided_at,
  ADD COLUMN execution_finished_at datetime DEFAULT NULL AFTER execution_started_at;

CREATE INDEX idx_approval_binding ON approval_request(tenant_id, shop_id, tool_code, business_object_id, input_hash);

CREATE TABLE write_operation (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  shop_id bigint NOT NULL,
  user_id bigint NOT NULL,
  task_id bigint DEFAULT NULL,
  trace_id varchar(128) DEFAULT NULL,
  tool_code varchar(128) NOT NULL,
  business_object_id varchar(128) NOT NULL,
  operation_request_id varchar(128) NOT NULL,
  idempotency_key varchar(512) NOT NULL,
  input_hash varchar(64) NOT NULL,
  approval_id bigint DEFAULT NULL,
  status varchar(32) NOT NULL,
  external_reference varchar(128) DEFAULT NULL,
  result_json json DEFAULT NULL,
  last_error_code varchar(64) DEFAULT NULL,
  last_error_message text DEFAULT NULL,
  retry_action varchar(32) DEFAULT NULL,
  version int NOT NULL DEFAULT 0,
  created_at datetime NOT NULL,
  updated_at datetime NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_write_operation_idempotency (idempotency_key),
  KEY idx_write_operation_reconcile (status, updated_at),
  KEY idx_write_operation_scope (tenant_id, shop_id, tool_code, business_object_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_event (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id bigint NOT NULL,
  shop_id bigint NOT NULL,
  aggregate_type varchar(64) NOT NULL,
  aggregate_id varchar(128) NOT NULL,
  event_type varchar(128) NOT NULL,
  payload_json json NOT NULL,
  status varchar(32) NOT NULL,
  attempt_count int NOT NULL DEFAULT 0,
  next_attempt_at datetime NOT NULL,
  last_error text DEFAULT NULL,
  claimed_by varchar(128) DEFAULT NULL,
  claimed_at datetime DEFAULT NULL,
  published_at datetime DEFAULT NULL,
  created_at datetime NOT NULL,
  updated_at datetime NOT NULL,
  PRIMARY KEY (id),
  KEY idx_outbox_publish (status, next_attempt_at),
  KEY idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE mcp_tool
SET idempotent = 1,
    retry_count = 0,
    risk_level = 'HIGH',
    need_approval = 1,
    permission_code = 'order:refund',
    updated_at = NOW()
WHERE tool_code = 'order.refund_execute';
