# ShopOps Agent 企业级数据库 ER 设计

## 1. 设计原则

ShopOps 数据库设计围绕企业级 Agent 运营中台展开，核心原则如下：

- 所有核心业务表必须包含 `tenant_id`，保证租户隔离。
- 店铺相关数据必须包含 `shop_id`，保证多店铺数据边界。
- Agent 不直接访问业务表，所有数据访问通过 Tool Gateway 和工具执行器完成。
- 工具调用、模型调用、审批操作、报告生成必须形成可追踪链路。
- 高风险动作必须先生成审批记录，再由审批结果驱动后续执行。
- 任务和步骤状态必须可恢复，支持异步执行、失败重试和降级。
- 第一版可兼容 mall-master 的 `ums_*`、`oms_*`、`pms_*` 表作为模拟电商数据源。

## 2. 核心实体关系

```text
tenant
  ├── tenant_member
  ├── shop
  │     ├── shop_member
  │     ├── shop_config
  │     ├── connector_account
  │     ├── agent_task
  │     │     ├── agent_task_step
  │     │     │     ├── tool_call_log
  │     │     │     └── approval_record
  │     │     ├── model_call_log
  │     │     └── operation_report
  │     └── report_export_log
  ├── mcp_tool
  │     ├── tool_version
  │     ├── tool_rate_limit
  │     └── tool_risk_policy
  ├── prompt_template
  └── model_provider
```

## 3. 租户与用户域

### 3.1 tenant

企业或团队租户。

```sql
CREATE TABLE tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_no VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  plan_type VARCHAR(32) DEFAULT NULL,
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_tenant_no (tenant_no)
);
```

### 3.2 tenant_member

用户和租户关系。用户表可复用 mall-master 的 `ums_admin`，也可以改名为 `sys_user`。

```sql
CREATE TABLE tenant_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  joined_at DATETIME NOT NULL,
  UNIQUE KEY uk_tenant_user (tenant_id, user_id),
  KEY idx_user_id (user_id)
);
```

## 4. 店铺域

### 4.1 shop

```sql
CREATE TABLE shop (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_no VARCHAR(64) NOT NULL,
  shop_name VARCHAR(128) NOT NULL,
  platform_type VARCHAR(32) NOT NULL,
  owner_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_tenant_shop_no (tenant_id, shop_no),
  KEY idx_tenant_id (tenant_id)
);
```

### 4.2 shop_member

```sql
CREATE TABLE shop_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  joined_at DATETIME NOT NULL,
  UNIQUE KEY uk_shop_user (shop_id, user_id),
  KEY idx_tenant_user (tenant_id, user_id)
);
```

### 4.3 shop_config

存储店铺运营规则、指标阈值、审批策略等配置。

```sql
CREATE TABLE shop_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value TEXT NOT NULL,
  value_type VARCHAR(32) NOT NULL DEFAULT 'string',
  updated_by BIGINT DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_shop_config (shop_id, config_key),
  KEY idx_tenant_shop (tenant_id, shop_id)
);
```

## 5. 连接器域

### 5.1 platform_connector

定义可接入的平台连接器。

```sql
CREATE TABLE platform_connector (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  connector_code VARCHAR(64) NOT NULL,
  connector_name VARCHAR(128) NOT NULL,
  platform_type VARCHAR(32) NOT NULL,
  description VARCHAR(512) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_connector_code (connector_code)
);
```

### 5.2 connector_account

店铺绑定的外部平台账号或模拟数据源账号。

```sql
CREATE TABLE connector_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  connector_code VARCHAR(64) NOT NULL,
  account_name VARCHAR(128) NOT NULL,
  auth_type VARCHAR(32) NOT NULL,
  auth_config_json JSON DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_sync_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  KEY idx_tenant_shop (tenant_id, shop_id),
  KEY idx_connector_code (connector_code)
);
```

### 5.3 connector_sync_log

```sql
CREATE TABLE connector_sync_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  connector_code VARCHAR(64) NOT NULL,
  sync_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME DEFAULT NULL,
  error_message TEXT DEFAULT NULL,
  KEY idx_shop_connector (shop_id, connector_code),
  KEY idx_status (status)
);
```

## 6. MCP 工具域

### 6.1 mcp_tool

工具资产主表。

```sql
CREATE TABLE mcp_tool (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT DEFAULT NULL,
  tool_code VARCHAR(128) NOT NULL,
  tool_name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  description TEXT DEFAULT NULL,
  input_schema JSON NOT NULL,
  output_schema JSON NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  need_approval TINYINT NOT NULL DEFAULT 0,
  idempotent TINYINT NOT NULL DEFAULT 1,
  timeout_ms INT NOT NULL DEFAULT 10000,
  retry_count INT NOT NULL DEFAULT 0,
  connector_code VARCHAR(64) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
  owner VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_tool_tenant_code_version (tenant_id, tool_code, version),
  KEY idx_tool_code (tool_code),
  KEY idx_permission_code (permission_code),
  KEY idx_risk_level (risk_level)
);
```

说明：

- `tenant_id` 为空表示平台全局工具。
- `tenant_id` 不为空表示租户自定义工具。

### 6.2 tool_version

```sql
CREATE TABLE tool_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tool_id BIGINT NOT NULL,
  version VARCHAR(32) NOT NULL,
  input_schema JSON NOT NULL,
  output_schema JSON NOT NULL,
  change_log TEXT DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_tool_version (tool_id, version)
);
```

### 6.3 tool_call_log

工具调用审计表。

```sql
CREATE TABLE tool_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  task_id BIGINT DEFAULT NULL,
  step_id BIGINT DEFAULT NULL,
  trace_id VARCHAR(128) NOT NULL,
  span_id VARCHAR(128) NOT NULL,
  user_id BIGINT NOT NULL,
  tool_code VARCHAR(128) NOT NULL,
  tool_version VARCHAR(32) DEFAULT NULL,
  input_json JSON DEFAULT NULL,
  output_json JSON DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  risk_level VARCHAR(32) DEFAULT NULL,
  approval_id BIGINT DEFAULT NULL,
  latency_ms INT DEFAULT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  error_code VARCHAR(64) DEFAULT NULL,
  error_message TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_trace_id (trace_id),
  KEY idx_task_step (task_id, step_id),
  KEY idx_shop_tool (shop_id, tool_code),
  KEY idx_status (status)
);
```

## 7. Agent 任务域

### 7.1 agent_task

```sql
CREATE TABLE agent_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  task_no VARCHAR(64) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  user_input TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  priority INT NOT NULL DEFAULT 5,
  plan_json JSON DEFAULT NULL,
  result_summary TEXT DEFAULT NULL,
  trace_id VARCHAR(128) NOT NULL,
  error_code VARCHAR(64) DEFAULT NULL,
  error_message TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_tenant_shop (tenant_id, shop_id),
  KEY idx_user_status (user_id, status),
  KEY idx_trace_id (trace_id),
  KEY idx_created_at (created_at)
);
```

### 7.2 agent_task_step

```sql
CREATE TABLE agent_task_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  step_no INT NOT NULL,
  step_name VARCHAR(128) DEFAULT NULL,
  tool_code VARCHAR(128) DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  depends_on VARCHAR(256) DEFAULT NULL,
  input_json JSON DEFAULT NULL,
  output_json JSON DEFAULT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  approval_id BIGINT DEFAULT NULL,
  error_code VARCHAR(64) DEFAULT NULL,
  error_message TEXT DEFAULT NULL,
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_task_step_no (task_id, step_no),
  KEY idx_task_id (task_id),
  KEY idx_status (status),
  KEY idx_tool_code (tool_code)
);
```

### 7.3 agent_task_event

记录任务状态变化事件，方便排查和前端时间线展示。

```sql
CREATE TABLE agent_task_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  from_status VARCHAR(32) DEFAULT NULL,
  to_status VARCHAR(32) DEFAULT NULL,
  event_data_json JSON DEFAULT NULL,
  operator_id BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_task_id (task_id),
  KEY idx_event_type (event_type)
);
```

## 8. 审批域

### 8.1 approval_record

```sql
CREATE TABLE approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  task_id BIGINT DEFAULT NULL,
  step_id BIGINT DEFAULT NULL,
  approval_no VARCHAR(64) NOT NULL,
  approval_type VARCHAR(64) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  original_content_json JSON NOT NULL,
  modified_content_json JSON DEFAULT NULL,
  evidence_json JSON DEFAULT NULL,
  risk_reason TEXT DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  applicant_id BIGINT NOT NULL,
  approver_id BIGINT DEFAULT NULL,
  approval_comment TEXT DEFAULT NULL,
  execution_status VARCHAR(32) DEFAULT NULL,
  execution_result_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL,
  approved_at DATETIME DEFAULT NULL,
  executed_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_approval_no (approval_no),
  KEY idx_task_step (task_id, step_id),
  KEY idx_shop_status (shop_id, status),
  KEY idx_approver_status (approver_id, status)
);
```

### 8.2 approval_action_log

```sql
CREATE TABLE approval_action_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  approval_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  operator_id BIGINT NOT NULL,
  comment TEXT DEFAULT NULL,
  before_json JSON DEFAULT NULL,
  after_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_approval_id (approval_id)
);
```

## 9. 模型与 Prompt 域

### 9.1 model_provider

```sql
CREATE TABLE model_provider (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT DEFAULT NULL,
  provider_code VARCHAR(64) NOT NULL,
  provider_name VARCHAR(128) NOT NULL,
  base_url VARCHAR(512) DEFAULT NULL,
  api_key_secret VARCHAR(256) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_tenant_provider (tenant_id, provider_code)
);
```

### 9.2 prompt_template

```sql
CREATE TABLE prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT DEFAULT NULL,
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
  KEY idx_prompt_code (prompt_code)
);
```

### 9.3 model_call_log

```sql
CREATE TABLE model_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT DEFAULT NULL,
  task_id BIGINT DEFAULT NULL,
  step_id BIGINT DEFAULT NULL,
  trace_id VARCHAR(128) NOT NULL,
  span_id VARCHAR(128) NOT NULL,
  user_id BIGINT DEFAULT NULL,
  provider_code VARCHAR(64) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  prompt_code VARCHAR(128) DEFAULT NULL,
  prompt_version VARCHAR(32) DEFAULT NULL,
  input_tokens INT DEFAULT NULL,
  output_tokens INT DEFAULT NULL,
  latency_ms INT DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  error_message TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_trace_id (trace_id),
  KEY idx_task_step (task_id, step_id),
  KEY idx_provider_model (provider_code, model_name),
  KEY idx_status (status)
);
```

## 10. 报告域

### 10.1 operation_report

```sql
CREATE TABLE operation_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  task_id BIGINT DEFAULT NULL,
  report_no VARCHAR(64) NOT NULL,
  report_type VARCHAR(64) NOT NULL,
  title VARCHAR(256) NOT NULL,
  content_markdown MEDIUMTEXT DEFAULT NULL,
  content_json JSON DEFAULT NULL,
  evidence_json JSON DEFAULT NULL,
  trace_id VARCHAR(128) DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  UNIQUE KEY uk_report_no (report_no),
  KEY idx_task_id (task_id),
  KEY idx_shop_type (shop_id, report_type),
  KEY idx_trace_id (trace_id)
);
```

### 10.2 report_export_log

```sql
CREATE TABLE report_export_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  report_id BIGINT NOT NULL,
  export_type VARCHAR(32) NOT NULL,
  export_url VARCHAR(512) DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  operator_id BIGINT NOT NULL,
  error_message TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_report_id (report_id),
  KEY idx_shop_id (shop_id)
);
```

## 11. Trace 与审计域

### 11.1 trace_span

统一记录任务、模型、工具、审批、报告生成等调用链节点。

```sql
CREATE TABLE trace_span (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT DEFAULT NULL,
  trace_id VARCHAR(128) NOT NULL,
  span_id VARCHAR(128) NOT NULL,
  parent_span_id VARCHAR(128) DEFAULT NULL,
  span_type VARCHAR(64) NOT NULL,
  span_name VARCHAR(128) NOT NULL,
  ref_type VARCHAR(64) DEFAULT NULL,
  ref_id BIGINT DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  input_summary TEXT DEFAULT NULL,
  output_summary TEXT DEFAULT NULL,
  latency_ms INT DEFAULT NULL,
  error_message TEXT DEFAULT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME DEFAULT NULL,
  KEY idx_trace_id (trace_id),
  KEY idx_parent_span (parent_span_id),
  KEY idx_ref (ref_type, ref_id)
);
```

## 12. 状态枚举建议

### 12.1 agent_task.status

```text
CREATED
PLANNING
PLAN_FAILED
QUEUED
RUNNING
WAITING_APPROVAL
APPROVED_EXECUTING
PARTIAL_SUCCESS
DEGRADED
SUCCESS
FAILED
CANCELLED
EXPIRED
```

### 12.2 agent_task_step.status

```text
PENDING
RUNNING
WAITING_APPROVAL
SUCCESS
FAILED
SKIPPED
DEGRADED
CANCELLED
```

### 12.3 approval_record.status

```text
PENDING
APPROVED
REJECTED
MODIFIED_APPROVED
CANCELLED
EXPIRED
EXECUTED
EXECUTE_FAILED
```

### 12.4 tool_call_log.status

```text
SUCCESS
FAILED
TIMEOUT
REJECTED_BY_PERMISSION
WAITING_APPROVAL
REJECTED_BY_RISK
DEGRADED
```

## 13. 与 mall-master 表的映射

第一版可以复用 mall-master 的业务表作为模拟电商平台数据源。

```text
ums_admin                  -> 平台用户
ums_role                   -> 平台角色
ums_resource               -> API 权限资源
ums_admin_role_relation    -> 用户角色关系
oms_order                  -> 订单数据源
oms_order_item             -> 订单明细数据源
pms_product                -> 商品数据源
pms_comment                -> 评论数据源
pms_product_operate_log    -> 商品操作记录参考
oms_order_operate_history  -> 订单操作记录参考
```

需要注意：

- mall 原表大多没有 `tenant_id` 和 `shop_id`，企业级 ShopOps 应通过扩展字段或映射表补齐边界。
- mall 的权限资源偏 API 权限，ShopOps 需要额外引入工具权限。
- mall 的操作记录偏业务行为日志，ShopOps 需要额外构建 trace/span 级调用链。

## 14. 第一版建表优先级

优先级 P0：

```text
tenant
tenant_member
shop
shop_member
mcp_tool
agent_task
agent_task_step
tool_call_log
operation_report
```

优先级 P1：

```text
approval_record
approval_action_log
prompt_template
model_provider
model_call_log
trace_span
```

优先级 P2：

```text
platform_connector
connector_account
connector_sync_log
tool_version
report_export_log
```

## 15. 总结

这套 ER 设计的重点不是简单存储 Agent 结果，而是支撑企业级 ShopOps 的治理闭环：租户隔离、店铺边界、工具资产、任务状态、审批风控、模型调用、报告沉淀和全链路审计。

后续后端实现时，可以先基于 P0 表跑通每日经营复盘任务，再逐步补齐审批、模型网关、Trace 和连接器体系。
