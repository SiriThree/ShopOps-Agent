# ShopOps Agent 企业级平台实现方案

## 1. 项目定位升级

ShopOps Agent 定位为面向电商企业、品牌商、多店铺运营团队的企业级智能运营中台。平台不只是一个运营问答机器人，而是一个融合电商业务数据、Agent 编排、MCP 工具治理、审批风控、审计追踪和报告沉淀的 ShopOps 平台。

平台核心目标是将订单、商品、评论、广告、库存、客服、报表和协作平台等运营能力封装为受控工具，让 Agent 在权限、租户、店铺、风险等级和审计约束下完成运营分析、异常发现、优化建议和高风险操作流转。

企业级定位关键词：

- 多租户：支持多个企业或团队独立使用。
- 多店铺：同一租户下支持淘宝、京东、拼多多、抖音、小红书等多店铺。
- 多平台连接器：通过 Connector 适配不同电商平台和内部系统。
- 工具治理：统一管理工具 Schema、权限、版本、风险、限流和审计。
- Agent 编排：支持 Planner、Executor、Verifier 的可控执行链路。
- 人工审批：高风险动作必须进入审批流。
- 全链路审计：完整记录用户输入、计划、工具调用、模型调用、审批和报告。
- 私有化部署：支持企业本地模型、内网数据源和 Docker/Kubernetes 部署。

## 2. 开源项目参考关系

ShopOps 可以参考多个成熟开源电商项目，但不应被任何一个项目限制架构边界。

### 2.1 mall / mall-master

`mall-master` 适合作为 Java 后端工程基座参考。它提供了 Spring Boot 多模块组织、后台管理 API、JWT 登录、Spring Security 动态权限、MyBatis 数据访问、RabbitMQ 异步消息、统一返回结构和完整电商业务表。

可借鉴内容：

- `mall-common`：统一返回、分页、异常、日志。
- `mall-security`：JWT 认证、动态资源权限、Spring Security 封装。
- `mall-mbg`：MyBatis Generator 实体和 Mapper 生成方式。
- `mall-admin`：后台管理接口分层。
- `oms_*`、`pms_*`、`ums_*`：订单、商品、评论、用户权限等基础数据模型。
- RabbitMQ 示例：异步任务、延迟队列、消息消费。

在 ShopOps 中，mall-master 应定位为电商后台和 Java 工程参考，而不是项目主体。

### 2.2 mall-swarm / mall4cloud

这类微服务电商项目适合参考企业级服务拆分和基础设施能力，例如网关、注册中心、配置中心、服务治理、消息队列、分布式事务、搜索、对象存储和运维监控。

ShopOps 第一版可以采用模块化单体实现，但服务边界应按微服务演进方式设计。

### 2.3 Headless Commerce / Vendure 类项目

Headless Commerce 项目强调核心商业能力 API 化、插件化和事件化。ShopOps 可以借鉴其扩展思想，将不同平台、不同运营动作、不同模型能力做成可注册、可治理、可替换的组件。

## 3. 总体架构

ShopOps 推荐采用“模块化单体起步，微服务可演进”的架构。第一版可以部署为一个 Spring Boot 应用，但代码和领域边界按企业级服务拆分。

```text
React / Ant Design Pro 运营工作台
        |
        v
API Gateway / Admin API
        |
        v
Spring Boot 后端平台
  ├── Auth & Tenant 用户、租户、角色、权限
  ├── Shop Center 店铺、平台账号、数据源配置
  ├── Connector Center 电商平台连接器
  ├── Tool Center MCP 工具注册与治理
  ├── Tool Gateway 工具调用网关
  ├── Agent Engine Planner / Executor / Verifier
  ├── Workflow 审批流与人工确认
  ├── Report Center 报告生成、导出、沉淀
  ├── Model Gateway 模型路由与调用记录
  └── Observability Trace、日志、指标、成本
        |
        v
MySQL / Redis / RabbitMQ / Elasticsearch / MinIO
        |
        v
Ollama / OpenAI-compatible API / DeepSeek / Qwen
        |
        v
淘宝 / 京东 / 拼多多 / 抖音 / 飞书 / 企业微信 / 模拟数据源
```

## 4. 推荐后端模块

第一阶段建议使用多模块 Maven 工程：

```text
shopops
├── shopops-common
├── shopops-mbg
├── shopops-security
├── shopops-admin
├── shopops-agent
├── shopops-tool
├── shopops-connector
├── shopops-workflow
├── shopops-report
├── shopops-model
└── shopops-observability
```

如果希望降低启动复杂度，也可以先将 `agent`、`tool`、`connector`、`workflow`、`report` 放在 `shopops-admin` 下作为独立包，后续再拆模块。

推荐包结构：

```text
com.shopops.auth
com.shopops.tenant
com.shopops.shop
com.shopops.connector
com.shopops.tool
com.shopops.agent
com.shopops.workflow
com.shopops.report
com.shopops.model
com.shopops.audit
```

## 5. 核心领域模型

### 5.1 租户域

租户是企业级隔离的第一边界。所有关键业务表必须包含 `tenant_id`。

核心表：

```text
tenant
tenant_member
tenant_config
```

设计重点：

- 用户可以属于多个租户。
- 租户拥有独立店铺、数据源、工具配置、审批策略和模型策略。
- 所有查询必须带租户边界。

### 5.2 店铺域

店铺是运营任务的业务边界。所有 Agent 任务、工具调用、报告和审批都应绑定 `shop_id`。

核心表：

```text
shop
shop_member
shop_config
shop_metric_threshold
```

设计重点：

- 一个租户下可以有多个店铺。
- 店铺绑定具体平台账号。
- 店铺有独立指标阈值、标题规则、风险策略和审批人。

### 5.3 连接器域

连接器负责适配外部平台或内部系统。

核心表：

```text
platform_connector
connector_account
connector_auth_token
connector_sync_log
```

连接器示例：

```text
taobao.order
jd.product
pdd.comment
douyin.ad
mock.mall
feishu.document
excel.local
```

设计重点：

- Connector 负责和外部平台交互。
- Tool 负责把 Connector 能力封装为 Agent 可调用动作。
- Agent 不直接访问 Connector，也不直接访问数据库。

## 6. MCP 工具治理

工具中心是 ShopOps 的核心能力之一。它不是简单的方法列表，而是企业级工具资产目录。

核心表：

```text
mcp_tool
tool_version
tool_permission
tool_rate_limit
tool_risk_policy
tool_call_log
```

`mcp_tool` 建议字段：

```text
id
tenant_id
tool_code
tool_name
description
category
input_schema
output_schema
permission_code
risk_level
need_approval
idempotent
timeout_ms
retry_count
rate_limit_config
connector_code
enabled
version
owner
created_at
updated_at
```

工具示例：

```text
order.query_summary
order.query_recent
comment.query_negative
comment.classify_sentiment
product.query_low_ctr
product.generate_title_candidates
product.update_title
ad.query_performance
ad.suggest_budget_adjustment
report.export_excel
feishu.sync_report
```

工具调用必须经过 Tool Gateway：

```text
Agent -> ToolGateway -> Permission Check -> Schema Check -> Risk Check -> Connector/Local Executor -> Output Check -> Audit Log
```

Tool Gateway 职责：

- 校验工具是否启用。
- 校验租户和店铺边界。
- 校验用户是否拥有工具权限。
- 校验输入 JSON Schema。
- 判断风险等级和审批策略。
- 执行限流、超时、重试和熔断。
- 校验输出 JSON Schema。
- 记录完整调用日志。

## 7. Agent 编排引擎

ShopOps Agent Engine 采用 Planner、Executor、Verifier 三段式设计。

### 7.1 Planner

Planner 负责将用户自然语言任务转换为结构化执行计划。

输入：

```text
tenant_id
shop_id
user_id
user_input
available_tools
shop_config
permission_context
history_context
```

输出：

```json
{
  "task_type": "daily_review",
  "risk_level": "medium",
  "steps": [
    {
      "step_no": 1,
      "tool_code": "order.query_summary",
      "reason": "查询经营核心指标",
      "depends_on": []
    },
    {
      "step_no": 2,
      "tool_code": "comment.query_negative",
      "reason": "识别新增差评风险",
      "depends_on": []
    },
    {
      "step_no": 3,
      "tool_code": "report.generate_daily_review",
      "reason": "生成经营复盘报告",
      "depends_on": [1, 2]
    }
  ]
}
```

Planner 输出必须经过后端 Plan Validator 校验：

- 工具是否存在。
- 用户是否有候选工具权限。
- 依赖关系是否形成环。
- 高风险工具是否进入审批节点。
- 参数是否可由上下文生成。

### 7.2 Executor

Executor 负责执行结构化计划。

能力：

- 顺序执行。
- 并行执行。
- DAG 依赖执行。
- 失败重试。
- 超时控制。
- 中间结果缓存。
- 状态持久化。
- 支持取消任务。

Executor 不直接调用业务代码，只调用 Tool Gateway。

### 7.3 Verifier

Verifier 负责校验执行结果和最终输出。

校验内容：

- 工具输出是否符合 Schema。
- 报告数值是否来自工具返回。
- 建议是否有证据数据。
- 高风险动作是否进入审批。
- 是否出现无依据结论。
- 是否违反平台规则或禁用词。
- 是否存在数据缺失且未标注。

校验失败处理：

- 重新生成报告。
- 重试失败工具。
- 标记部分成功。
- 降级为模板报告。
- 转人工处理。

## 8. 任务状态机

企业级任务状态建议比普通 demo 更细。

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

核心表：

```text
agent_task
agent_task_step
agent_task_event
```

`agent_task` 建议字段：

```text
id
tenant_id
shop_id
user_id
task_no
task_type
user_input
status
priority
plan_json
result_summary
trace_id
error_code
error_message
created_at
started_at
finished_at
```

`agent_task_step` 建议字段：

```text
id
tenant_id
task_id
step_no
step_name
tool_code
status
depends_on
input_json
output_json
retry_count
approval_id
error_message
started_at
finished_at
```

## 9. 审批与风控

高风险操作必须进入审批流。审批不是简单的通过/拒绝，而是企业治理能力。

需要审批的场景：

- 修改商品标题。
- 上下架商品。
- 调整投放预算。
- 批量导出敏感数据。
- 同步报告到外部协作平台。
- 发送补偿、退款或售后建议。

审批能力：

- 通过。
- 驳回。
- 修改后通过。
- 分级审批。
- 超时取消。
- 审批前后 diff。
- 审批意见记录。
- 审批后执行结果回写。

核心表：

```text
approval_record
approval_action_log
approval_policy
```

`approval_record` 建议字段：

```text
id
tenant_id
shop_id
task_id
step_id
approval_type
risk_level
original_content_json
modified_content_json
evidence_json
risk_reason
status
applicant_id
approver_id
approval_comment
execution_status
execution_result_json
created_at
approved_at
executed_at
```

## 10. 模型网关

Model Gateway 用于屏蔽不同模型供应商差异，并统一做路由、日志、降级和成本统计。

支持模型：

```text
Ollama 本地模型
OpenAI-compatible API
DeepSeek
通义千问
智谱
企业私有模型
```

接口抽象：

```java
public interface ModelClient {
    ChatResponse chat(ChatRequest request);
    JsonResponse generateJson(JsonRequest request);
}
```

路由策略：

- 敏感数据优先走本地模型。
- 复杂报告生成走强模型。
- 低成本分类任务走小模型。
- 模型超时切换备用模型。
- 模型不可用时降级为规则或模板。

核心表：

```text
model_provider
model_route_policy
prompt_template
model_call_log
```

## 11. 报告中心

报告不只是任务输出，而是企业运营资产。

报告类型：

- 每日经营复盘。
- 差评舆情报告。
- 商品标题优化报告。
- 投放复盘报告。
- 异常告警报告。
- 自定义分析报告。

报告结构：

```text
核心指标
异常发现
原因分析
优化建议
证据数据
工具调用链摘要
模型调用摘要
审批记录
数据缺失说明
```

导出方式：

- Web 查看。
- Markdown。
- Excel。
- PDF。
- 飞书文档。
- 企业微信消息。

核心表：

```text
operation_report
report_export_log
report_evidence
```

## 12. 可观测性与审计

ShopOps 必须回答一个关键问题：Agent 为什么这么做？

一次任务应形成完整 Trace：

```text
task trace
├── user input
├── planner model call
├── plan validation
├── tool call: order.query_summary
├── tool call: comment.query_negative
├── report generation model call
├── verifier check
├── approval record
└── final report
```

核心能力：

- trace_id 全链路贯穿。
- 工具调用日志。
- 模型调用日志。
- 审批日志。
- 报告证据来源。
- 失败原因定位。
- 成本、耗时、成功率统计。

核心指标：

```text
任务成功率
任务平均耗时
工具调用成功率
模型调用成功率
审批通过率
降级次数
异常指标召回率
平均 token 成本
租户级调用量
工具级错误率
```

## 13. 企业级非功能设计

### 13.1 安全

- JWT 登录认证。
- RBAC 权限。
- 租户隔离。
- 店铺数据权限。
- 工具权限二次校验。
- 敏感字段脱敏。
- 高风险操作审批。
- API 限流。

### 13.2 幂等

写操作必须支持幂等：

- 工具调用携带 `idempotency_key`。
- 审批通过后的执行动作只允许成功执行一次。
- 消息重复消费不应造成重复修改。

### 13.3 限流与熔断

- 租户级限流。
- 用户级限流。
- 工具级限流。
- 连接器级限流。
- 模型供应商熔断。

### 13.4 降级

- LLM 不可用时使用模板报告。
- 分类任务可降级为规则匹配。
- 单个工具失败时报告标注数据缺失。
- 外部平台限流时延迟重试。

### 13.5 部署

第一阶段：

```text
Docker Compose
Spring Boot
MySQL
Redis
RabbitMQ
MinIO
Ollama
```

企业部署目标：

```text
Kubernetes
Ingress / Gateway
Prometheus
Grafana
ELK / OpenSearch
OpenTelemetry
```

## 14. 第一阶段落地路线

即使目标是企业级，第一阶段也应选择清晰的企业级最小闭环。

阶段一：后端基座

- 搭建 Spring Boot 多模块工程。
- 复用 mall-master 的统一返回、分页、权限设计。
- 实现用户、租户、店铺、角色权限。

阶段二：电商模拟数据源

- 复用 mall 的 `oms_order`、`pms_product`、`pms_comment`。
- 增加店铺和租户字段或通过映射表隔离。
- 实现订单、商品、评论查询服务。

阶段三：工具中心

- 实现 `mcp_tool`。
- 实现 Tool Gateway。
- 实现订单、评论、商品、报告类工具。
- 完成工具权限、Schema、风险校验。

阶段四：Agent 任务闭环

- 实现 `agent_task`、`agent_task_step`。
- 实现 Planner、Executor、Verifier。
- 跑通每日经营复盘。

阶段五：异步与审批

- 引入 RabbitMQ。
- 实现任务队列、重试队列、死信队列。
- 实现高风险操作审批流。

阶段六：企业治理

- 实现调用链 Trace。
- 实现模型调用日志。
- 实现报告证据来源。
- 增加限流、幂等、降级和监控指标。

## 15. 面试与项目表述

推荐表述：

> ShopOps Agent 是一个面向多店铺电商运营团队的企业级智能运营中台。项目参考 mall、mall-swarm、mall4cloud 等开源电商系统的后端架构和业务模型，基于 Spring Boot、MySQL、Redis、RabbitMQ 和 React 构建。平台将订单、商品、评论、广告、报表和协作平台能力抽象为可治理的 MCP 工具，通过 Planner-Executor-Verifier Agent 链路实现自然语言任务拆解、工具调用、结果校验和报告生成，并通过租户隔离、工具权限、人工审批、全链路审计、模型网关和降级机制保障 Agent 在企业场景下可控、可追踪、可运维。

与普通 AI Demo 的区别：

- 不是单轮问答，而是平台级任务系统。
- 不是直接让大模型访问数据，而是通过工具网关受控调用。
- 不是只生成文本，而是沉淀结构化报告和证据链。
- 不是完全自动执行，而是高风险动作进入审批流。
- 不是只关注模型效果，而是关注权限、审计、降级、成本和可观测性。

## 16. 总结

ShopOps 的企业级价值不在于“会调用大模型”，而在于把 Agent 放进真实电商运营系统所需要的治理框架中。mall-master 可以提供成熟的 Java 电商后端参考，但 ShopOps 的核心竞争力应建立在 Agent 编排、工具治理、多租户隔离、审批风控、调用链审计和模型网关之上。

最终目标是形成一个可私有化部署、可扩展连接器、可管理工具资产、可审计 Agent 决策过程的电商智能运营中台。
