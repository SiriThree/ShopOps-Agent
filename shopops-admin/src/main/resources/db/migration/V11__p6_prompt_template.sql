CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  prompt_code VARCHAR(128) NOT NULL,
  prompt_name VARCHAR(128) NOT NULL,
  task_type VARCHAR(64) DEFAULT NULL,
  template_content TEXT NOT NULL,
  version VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_prompt_tenant_code_version (tenant_id, prompt_code, version),
  KEY idx_prompt_tenant_code_status (tenant_id, prompt_code, status),
  KEY idx_prompt_task_type (task_type)
);
