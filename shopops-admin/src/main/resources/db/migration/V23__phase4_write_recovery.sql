ALTER TABLE write_operation
  ADD COLUMN recovery_attempt_count int NOT NULL DEFAULT 0 AFTER retry_action,
  ADD COLUMN last_recovery_at datetime DEFAULT NULL AFTER recovery_attempt_count;

CREATE INDEX idx_write_operation_recovery_attempt ON write_operation(status, recovery_attempt_count, updated_at);
