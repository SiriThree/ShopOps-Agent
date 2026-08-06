# ShopOps 架构与关键链路

本文只描述当前仓库中实际存在的模块和阶段 0—6 已接入的治理能力。

## 1. 系统上下文

```mermaid
flowchart LR
  Operator[运营人员] --> UI[React Admin]
  Admin[平台管理员] --> UI
  UI --> API[shopops-admin]
  API --> MySQL[(MySQL)]
  API --> Redis[(Redis 健康/依赖)]
  API --> Rabbit[RabbitMQ]
  API --> Files[公开数据文件]
  API --> Webhook[Feishu Webhook 可选]
  API --> Model[OpenAI-compatible Model 可选]
```

## 2. 模块架构

```mermaid
flowchart TB
  API[Controller/API] --> Context[RequestContext + AuthorizationService]
  API --> Business[Business Services]
  API --> Task[Task / Approval / Audit]
  API --> Connector[Connector Services]
  API --> Agent[Agent Engine]
  Agent --> Template[WorkflowTemplateRegistry]
  Agent --> Planner[Planner]
  Planner --> Validator[Plan Validator]
  Validator --> Executor[Sequential Executor]
  Executor --> Gateway[Tool Gateway]
  Gateway --> Approval[Approval Service]
  Gateway --> Tool[Tool Executors]
  Tool --> Business
  Tool --> Connector
  Tool --> Reliability[Write Operation / Outbox / Reconciliation]
```

## 3. 数据与租户边界

```mermaid
flowchart LR
  Token[Verified Token] --> Subject[tenantId + userId]
  Subject --> Membership[Shop Membership Check]
  Membership --> Context[accessibleShopIds + currentShopId]
  Context --> Permission[roles + permissions + dataScope]
  Permission --> Controller
  Permission --> Service
  Permission --> Mapper[tenant/shop constrained query]
  Permission --> ToolGateway[Tool re-authorization]
  Permission --> Worker[Persisted task identity re-check]
```

前端传入的 tenantId、userId 和 role 不能成为后端可信身份。当前 shopId 是用户选择上下文，必须通过成员关系检查。

## 4. Agent 工作流

```mermaid
flowchart TD
  Create[Create Agent Task] --> Mode[Resolve execution mode]
  Mode --> Workflow[Match controlled workflow]
  Workflow --> Plan[Planner]
  Plan --> Validate[Plan Validator]
  Validate -->|reject| Failed[Explainable failure]
  Validate --> Execute[Executor]
  Execute --> Gateway[Tool Gateway]
  Gateway --> Auth[Permission/Risk/Approval]
  Auth --> Tool[Tool Executor]
  Tool --> Verify[Verifier]
  Verify -->|pass| Report[Report/Trace/Audit]
  Verify -->|repairable| Repair[Limited repair plan]
  Repair --> Validate
  Verify -->|exhausted| Manual[Manual action]
```

## 5. 异步任务时序

```mermaid
sequenceDiagram
  participant API
  participant DB
  participant MQ as RabbitMQ
  participant W as Worker
  API->>DB: persist task/identity/status
  API->>MQ: publish task message
  MQ->>W: deliver message
  W->>DB: compare message with persisted identity
  W->>DB: CAS acquire lease
  alt lease acquired
    W->>DB: RUNNING + attempt
    W->>W: execute governed steps
    W->>DB: terminal/retry/manual status
  else duplicate or active lease
    W-->>MQ: no business execution
  end
```

当前发布仍存在需要 Outbox/confirm 继续完善的部分，周期心跳和完整自动接管尚未全部打通。

## 6. 高风险审批时序

```mermaid
sequenceDiagram
  participant Agent
  participant Gateway
  participant Approval
  participant User
  participant Write
  participant External
  Agent->>Gateway: tool + tenant/shop/user + args
  Gateway->>Gateway: permission and risk check
  Gateway->>Approval: create request + parameter digest
  User->>Approval: approve/reject
  Approval->>Approval: single-decision CAS
  Approval->>Gateway: approved request
  Gateway->>Gateway: compare execution digest
  Gateway->>Write: idempotency claim
  Write->>External: side effect outside DB transaction
  alt timeout/unknown
    Write->>Write: EXTERNAL_UNKNOWN
    Write->>External: status query before retry
  else success
    Write->>Write: local confirm + outbox + audit
  end
```

## 7. 连接器同步

```mermaid
flowchart LR
  Config[Connector config] --> Credential[Encrypted credential]
  Credential --> Read[File/API client]
  Read --> Page[Page + cursor]
  Page --> Validate[Mapping/basic validation]
  Validate --> Fingerprint[External ID + SHA-256]
  Fingerprint --> Upsert[Unique-key UPSERT]
  Upsert --> Checkpoint[Cursor/checkpoint]
  Checkpoint --> Log[Sync state/API log/audit]
```

当前深度实现是 `file.order-summary`，数据进入同步暂存记录，不直接污染核心订单表。

## 8. 可观测性链路

```mermaid
flowchart LR
  HTTP[HTTP requestId/traceId] --> Log[MDC application log]
  HTTP --> Metrics[Micrometer HTTP metrics]
  HTTP --> Task[taskId]
  Task --> Agent[Agent TraceSpan]
  Agent --> Tool[toolCallId]
  Tool --> Approval[approvalId]
  Tool --> Connector[connectorCallId]
  Task --> Audit[Audit timeline]
  Metrics --> Prom[Prometheus endpoint]
  Health[Actuator liveness/readiness] --> Platform[Deployment routing]
```

当前不是完整 OpenTelemetry 分布式 Trace；RabbitMQ W3C Trace Context 传播仍待补充。
