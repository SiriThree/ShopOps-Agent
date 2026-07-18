# ShopOps Agent：基于 MCP 的电商运营智能体平台详细设计报告

## 1. 项目定位

ShopOps Agent 是一个面向中小电商团队的运营智能体平台。项目不是单纯做一个能回答运营问题的聊天机器人，而是构建一个平台级系统，将店铺管理、用户权限、MCP 工具注册、Agent 任务编排、异步调度、人工审批、调用链审计和运营报告管理统一起来。

平台的核心目标是把电商运营中的高频工作流自动化，例如每日经营复盘、差评舆情监控、商品标题优化、客服话术生成、投放数据复盘等。运营人员通过自然语言发起任务，Agent 负责理解任务、拆解步骤、调用工具、聚合数据、生成报告；平台后端负责权限、任务状态、审批流、日志审计和结果沉淀。

项目适配岗位：

- Java 后端开发。
- AI 应用开发。
- 全栈开发。
- Agent 工程化开发。

## 2. 业务背景

中小电商运营团队通常需要同时处理多个平台的数据和工作：

- 店铺后台：订单、退款、商品、库存。
- 评论系统：差评、追评、舆情风险。
- 广告平台：消耗、点击、转化、ROI。
- Excel：运营日报、商品数据、投放复盘。
- 飞书/企业微信：报告同步、团队协作。

传统流程中，运营人员每天需要手动导出数据、筛选异常、编写复盘结论、生成客服话术，并在不同系统间来回切换。这个过程耗时、重复、易遗漏，并且缺少统一的操作审计。

ShopOps Agent 将这些平台能力封装为标准化工具，让 Agent 在受控边界内完成数据获取和分析，将“人工跨平台操作”变成“Agent 调用工具 + 人工确认高风险动作”。

## 3. 核心痛点

### 3.1 数据分散

订单、商品、评论、投放和报表数据分散在不同平台，运营人员难以快速形成完整经营视图。

### 3.2 复盘效率低

每日经营复盘依赖人工导出和汇总，耗时长，异常指标容易被遗漏。

### 3.3 操作风险高

商品标题修改、投放计划调整、客服补偿建议等操作存在业务风险，不能完全交给大模型自动执行。

### 3.4 Agent 不可控

如果只做“自然语言 -> 大模型回答”，无法知道 Agent 为什么调用某个工具、用了哪些数据、结论是否有依据。

### 3.5 缺少平台治理

真实企业系统需要用户权限、店铺隔离、任务状态、操作审计、失败重试、人工审批和模型降级，而普通 demo 往往没有这些能力。

## 4. 项目目标

业务目标：

- 支持运营人员通过自然语言创建运营任务。
- 自动完成订单、评论、商品、投放等数据分析。
- 生成结构化日报、异常告警、优化建议和客服话术。
- 支持高风险操作人工确认。
- 支持报告导出和团队协作沉淀。

技术目标：

- 使用 Java Spring Boot 构建平台级后端。
- 使用 React 构建运营工作台。
- 设计 MCP 风格工具注册中心。
- 构建 Agent Planner-Executor-Verifier 执行链路。
- 支持异步任务、失败重试、任务状态追踪。
- 支持用户权限、店铺隔离和人工审批。
- 支持 Ollama 本地模型与 OpenAI-compatible 模型切换。
- 记录完整调用链，保证可审计、可复盘、可降级。

## 5. 总体架构

```text
React 运营工作台
  -> Spring Boot 平台后端
      -> 用户与权限模块
      -> 店铺管理模块
      -> Agent 任务模块
      -> MCP 工具注册中心
      -> 工具调用网关
      -> 审批流模块
      -> 报告管理模块
      -> 日志审计模块
      -> 模型网关 Model Gateway
  -> Redis / MySQL / RabbitMQ
  -> Ollama / OpenAI-compatible LLM
  -> Python Agent Worker 可选
  -> 模拟电商平台 / Excel / 飞书
```

架构分层：

- 前端层：React 运营工作台。
- 接口层：Spring Boot RESTful API。
- 业务层：用户、店铺、任务、审批、报告、日志。
- Agent 层：任务拆解、工具路由、结果校验、输出生成。
- 工具层：订单、评论、商品、投放、Excel、飞书等工具。
- 数据层：MySQL、Redis、对象存储或本地文件。
- 模型层：Ollama 本地模型和云端模型。

## 6. 技术选型

后端：

- Java 17。
- Spring Boot。
- Spring Security 或 Sa-Token。
- MyBatis-Plus。
- MySQL。
- Redis。
- RabbitMQ。
- XXL-JOB 或 Quartz。
- Lombok。
- Knife4j / Swagger。

前端：

- React。
- TypeScript。
- Axios。
- ECharts。
- Ant Design 或 shadcn/ui。

Agent 与模型：

- Ollama 本地模型。
- OpenAI-compatible API。
- 自定义 Model Gateway。
- Tool Calling / Function Calling。
- Planner-Executor-Verifier。

数据与文件：

- Excel 导入导出：EasyExcel。
- 报告导出：Markdown / PDF / Excel。
- 日志：Logback + 结构化日志。

部署：

- Docker。
- Docker Compose。
- Nginx。

## 7. 核心业务模块

### 7.1 用户与权限模块

功能：

- 用户注册/登录。
- 角色管理。
- 权限点管理。
- 店铺成员管理。
- API 鉴权。
- 工具调用权限校验。

角色示例：

- 店主：拥有店铺全部权限。
- 运营主管：可创建任务、审批高风险建议。
- 普通运营：可查看数据、生成报告。
- 客服：可查看评论和生成话术。
- 只读成员：仅可查看报告。

权限点示例：

- `order:read`
- `comment:read`
- `product:read`
- `product:title:suggest`
- `product:title:update`
- `ad:read`
- `report:export`
- `approval:review`

设计重点：

工具调用前必须做权限校验。Agent 不能绕过平台权限直接调用工具。

### 7.2 店铺管理模块

功能：

- 创建店铺。
- 店铺成员管理。
- 店铺数据源配置。
- 店铺指标阈值配置。
- 店铺运营规则配置。

配置示例：

- 退款率告警阈值。
- 差评告警阈值。
- 投放 ROI 下限。
- 商品标题长度限制。
- 高风险操作审批人。

设计重点：

所有任务、工具调用、报告和审批记录都需要绑定 `shop_id`，保证多店铺数据隔离。

### 7.3 Agent 任务模块

任务类型：

- 每日经营复盘。
- 差评舆情监控。
- 商品标题优化。
- 客服话术生成。
- 投放数据复盘。
- 自定义分析任务。

任务状态：

- `PENDING`：等待执行。
- `RUNNING`：执行中。
- `WAITING_APPROVAL`：等待人工确认。
- `SUCCESS`：执行成功。
- `FAILED`：执行失败。
- `CANCELLED`：已取消。
- `DEGRADED`：降级完成。

任务执行模式：

- 同步轻任务：简单查询、单轮话术生成。
- 异步重任务：日报生成、批量标题优化、投放复盘。
- 定时任务：每日复盘、定时差评扫描。

### 7.4 MCP 工具注册中心

工具注册中心是平台核心之一。它负责维护所有可被 Agent 调用的工具定义。

工具元数据：

```text
tool_code
tool_name
description
input_schema
output_schema
permission_code
risk_level
timeout_ms
retry_count
need_approval
enabled
version
```

工具分类：

- 订单工具。
- 评论工具。
- 商品工具。
- 投放工具。
- 报表工具。
- 协作工具。
- 模型工具。

示例工具：

```text
order.query_summary
查询指定时间范围内的 GMV、订单数、退款率、客单价。
```

```text
comment.query_negative
查询低星评论并按情绪和问题类型聚合。
```

```text
product.generate_title_candidates
根据商品信息、关键词和平台规则生成标题候选。
```

```text
report.export_excel
将运营复盘结果导出为 Excel。
```

### 7.5 工具调用网关

工具调用网关负责真正执行工具，并在执行前后做控制。

调用前：

- 校验工具是否启用。
- 校验用户权限。
- 校验店铺数据范围。
- 校验输入参数 Schema。
- 判断是否需要人工审批。

调用中：

- 设置超时。
- 记录开始时间。
- 执行具体工具。
- 捕获异常。

调用后：

- 校验输出 Schema。
- 记录调用日志。
- 更新任务步骤。
- 触发重试或降级。

设计重点：

Agent 只能通过工具调用网关访问外部能力，不能直接访问数据库或外部平台。

### 7.6 审批流模块

需要审批的场景：

- 修改商品标题。
- 调整投放预算。
- 批量导出敏感数据。
- 向外部协作平台同步报告。
- 发送补偿或售后建议。

审批流程：

1. Agent 生成建议。
2. 平台创建审批记录。
3. 前端展示建议内容、依据和风险提示。
4. 审批人确认、驳回或修改。
5. 审批通过后执行工具。
6. 记录审批和执行结果。

审批记录应包含：

- 原始建议。
- 生成依据。
- 风险等级。
- 审批人。
- 审批意见。
- 审批时间。
- 执行结果。

### 7.7 报告管理模块

报告类型：

- 每日经营日报。
- 差评处理报告。
- 商品标题优化报告。
- 投放复盘报告。
- 自定义分析报告。

报告结构：

- 核心指标。
- 异常发现。
- 原因分析。
- 优化建议。
- 证据数据。
- Agent 调用链摘要。
- 人工确认记录。

导出方式：

- Web 页面查看。
- Excel 导出。
- Markdown 导出。
- 飞书文档同步。

### 7.8 日志审计模块

审计对象：

- 用户登录。
- 任务创建。
- Agent 规划。
- 工具调用。
- 模型调用。
- 审批操作。
- 报告导出。
- 异常和降级。

审计目标：

- 能复盘一次任务从输入到输出的全流程。
- 能定位工具调用失败原因。
- 能分析模型输出是否可靠。
- 能统计成本、耗时和成功率。

## 8. Agent 编排设计

ShopOps Agent 使用 Planner-Executor-Verifier 架构。

### 8.1 Planner

负责理解用户任务并生成执行计划。

输入：

- 用户自然语言任务。
- 用户身份与权限。
- 当前店铺配置。
- 可用工具列表。
- 历史任务上下文。

输出：

```json
{
  "task_type": "daily_review",
  "steps": [
    {"tool": "order.query_summary", "reason": "获取订单核心指标"},
    {"tool": "comment.query_negative", "reason": "识别差评风险"},
    {"tool": "ad.query_performance", "reason": "分析投放 ROI"},
    {"tool": "report.generate", "reason": "生成复盘报告"}
  ],
  "risk_level": "medium"
}
```

### 8.2 Executor

负责按计划执行工具。

能力：

- 顺序执行。
- 并行执行。
- 失败重试。
- 超时控制。
- 中间结果缓存。
- 状态更新。

对于日报任务，订单、评论、商品和投放查询可以并行执行，提高效率。

### 8.3 Verifier

负责校验工具结果和最终输出。

校验内容：

- 工具输出是否符合 Schema。
- 报告是否包含核心指标。
- 数值是否来自真实工具返回。
- 建议是否有数据依据。
- 高风险操作是否进入审批。
- 输出中是否包含平台禁用词或无依据结论。

如果校验失败：

- 重新生成。
- 重新调用工具。
- 降级为模板报告。
- 转人工处理。

## 9. 核心业务流程设计

### 9.1 每日经营复盘流程

用户输入：

```text
帮我生成今天店铺运营复盘。
```

执行流程：

1. 创建 Agent 任务。
2. Planner 判断任务类型为日报复盘。
3. Executor 并行调用订单、商品、评论、投放工具。
4. 汇总 GMV、订单数、退款率、客单价、点击率、转化率、ROI。
5. 与昨日和近 7 日均值对比。
6. Verifier 检查指标完整性。
7. LLM 生成结构化报告。
8. 报告保存并可导出。

输出：

- 今日核心指标。
- 异常指标。
- 可能原因。
- 待处理事项。
- 数据证据。

### 9.2 差评舆情监控流程

触发方式：

- 用户手动发起。
- 定时任务触发。

执行流程：

1. 拉取新增评论。
2. 规则筛选低星和高风险关键词。
3. LLM 做情绪识别和问题分类。
4. 检索订单和商品上下文。
5. 生成客服处理建议。
6. 高风险评论进入人工处理队列。

分类示例：

- 商品质量。
- 物流慢。
- 客服态度。
- 描述不符。
- 售后退款。

### 9.3 商品标题优化流程

执行流程：

1. 查询商品基础信息和历史表现。
2. 筛选低点击率商品。
3. 检索关键词和平台规则。
4. 生成标题候选。
5. 校验长度、违禁词、重复词、关键词覆盖。
6. 输出优化建议。
7. 如果执行修改，进入审批流。

设计原则：

Agent 可以生成建议，但不能绕过审批直接修改商品。

### 9.4 投放数据复盘流程

执行流程：

1. 拉取广告计划数据。
2. 计算消耗、点击、转化、ROI。
3. 识别高消耗低转化计划。
4. 生成优化建议。
5. 高风险预算调整进入审批。

## 10. 数据库设计

### 10.1 用户与权限

`sys_user`

- id
- username
- password_hash
- phone
- email
- status
- created_at

`sys_role`

- id
- role_code
- role_name
- description

`sys_permission`

- id
- permission_code
- permission_name
- resource_type

`sys_user_role`

- user_id
- role_id

`sys_role_permission`

- role_id
- permission_id

### 10.2 店铺域

`shop`

- id
- shop_name
- platform_type
- owner_id
- status
- created_at

`shop_member`

- id
- shop_id
- user_id
- role_code
- joined_at

`shop_config`

- id
- shop_id
- config_key
- config_value

### 10.3 Agent 任务

`agent_task`

- id
- task_no
- shop_id
- user_id
- task_type
- user_input
- status
- priority
- started_at
- finished_at
- error_message
- created_at

`agent_task_step`

- id
- task_id
- step_no
- tool_code
- step_name
- status
- input_json
- output_json
- error_message
- started_at
- finished_at

### 10.4 工具注册与调用

`mcp_tool`

- id
- tool_code
- tool_name
- description
- input_schema
- output_schema
- permission_code
- risk_level
- timeout_ms
- retry_count
- need_approval
- enabled
- version

`tool_call_log`

- id
- task_id
- step_id
- shop_id
- user_id
- tool_code
- input_json
- output_json
- status
- latency_ms
- error_message
- created_at

### 10.5 审批与报告

`approval_record`

- id
- task_id
- shop_id
- approval_type
- risk_level
- content_json
- status
- applicant_id
- approver_id
- approval_comment
- created_at
- approved_at

`operation_report`

- id
- report_no
- task_id
- shop_id
- report_type
- title
- content_markdown
- content_json
- export_url
- created_at

### 10.6 模型与 Prompt

`prompt_template`

- id
- prompt_code
- prompt_name
- template_content
- version
- status
- created_at

`model_call_log`

- id
- task_id
- provider
- model_name
- prompt_code
- input_tokens
- output_tokens
- latency_ms
- status
- created_at

## 11. 接口设计

### 11.1 任务接口

```text
POST /api/agent/tasks
创建 Agent 任务
```

```text
GET /api/agent/tasks/{taskId}
查询任务详情
```

```text
GET /api/agent/tasks/{taskId}/steps
查询任务步骤
```

```text
POST /api/agent/tasks/{taskId}/cancel
取消任务
```

### 11.2 工具接口

```text
GET /api/tools
查询工具列表
```

```text
POST /api/tools/{toolCode}/invoke
手动调用工具，主要用于测试和调试
```

### 11.3 审批接口

```text
GET /api/approvals/pending
查询待审批列表
```

```text
POST /api/approvals/{approvalId}/approve
审批通过
```

```text
POST /api/approvals/{approvalId}/reject
审批驳回
```

### 11.4 报告接口

```text
GET /api/reports/{reportId}
查询报告详情
```

```text
POST /api/reports/{reportId}/export
导出报告
```

### 11.5 日志接口

```text
GET /api/tasks/{taskId}/trace
查询 Agent 调用链
```

```text
GET /api/tools/call-logs
查询工具调用日志
```

## 12. 异步任务设计

为什么需要异步：

- 运营日报需要多个平台数据。
- 批量商品标题优化耗时较长。
- 投放复盘需要聚合大量数据。
- LLM 调用存在不稳定延迟。

设计方案：

1. 用户创建任务。
2. 后端写入 `agent_task`。
3. 发送消息到 RabbitMQ。
4. Worker 消费任务。
5. 执行 Planner。
6. 并行或顺序调用工具。
7. 更新任务状态和步骤状态。
8. 生成报告。
9. 前端轮询或 WebSocket 展示进度。

重试策略：

- 网络错误：自动重试。
- 参数错误：不重试，记录失败。
- 模型超时：切换备用模型或降级。
- 外部平台限流：延迟重试。

## 13. Model Gateway 设计

统一模型网关屏蔽不同模型调用差异。

支持 Provider：

- Ollama。
- OpenAI-compatible API。
- DeepSeek。
- 通义千问。
- 智谱。

接口：

```java
public interface ModelClient {
    ChatResponse chat(ChatRequest request);
    JsonResponse generateJson(JsonRequest request);
}
```

模型路由策略：

- 敏感数据优先走 Ollama 本地模型。
- 复杂报告生成可走更强云端模型。
- 低成本分类任务使用小模型。
- 模型超时后切换备用模型。

记录内容：

- provider。
- model_name。
- prompt_version。
- latency_ms。
- token_usage。
- status。

## 14. Prompt 管理设计

Prompt 不写死在代码中，而是作为模板管理。

Prompt 类型：

- 任务规划 Prompt。
- 工具参数生成 Prompt。
- 运营报告生成 Prompt。
- 差评分类 Prompt。
- 标题优化 Prompt。
- 投放复盘 Prompt。
- Verifier 校验 Prompt。

管理能力：

- Prompt 版本号。
- 启用/停用。
- 回滚。
- 变量占位。
- 调用日志关联。

模板变量示例：

```text
{{shop_config}}
{{user_input}}
{{tool_list}}
{{metric_data}}
{{retrieved_rules}}
```

## 15. 权限与风险控制

风险分级：

- 低风险：只读查询，如订单汇总。
- 中风险：生成建议，如标题候选。
- 高风险：写操作，如修改商品标题、同步外部平台。

控制策略：

- 低风险工具：权限校验后自动执行。
- 中风险工具：自动执行，但输出风险提示。
- 高风险工具：必须创建审批记录。

安全边界：

- Agent 不直接操作数据库。
- Agent 不直接调用外部平台。
- 所有工具调用经过网关。
- 所有写操作经过审批。
- 所有结果可审计。

## 16. 降级机制

LLM 不可用：

- 使用模板生成基础报告。
- 使用规则完成差评分类。
- 将任务标记为 `DEGRADED`。

工具失败：

- 单工具失败不影响整体报告。
- 报告中标记数据缺失。
- 可稍后重试失败步骤。

知识不足：

- 不编造结论。
- 提示数据不足。
- 给出需要补充的数据。

高峰排队：

- 返回任务 ID。
- 展示排队状态。
- 支持取消任务。

## 17. 可观测性设计

核心指标：

- 任务成功率。
- 平均任务耗时。
- 工具调用成功率。
- 工具平均延迟。
- 模型调用成功率。
- 模型平均延迟。
- 审批通过率。
- 降级次数。
- 异常指标召回率。

日志维度：

- trace_id。
- task_id。
- shop_id。
- user_id。
- tool_code。
- model_name。
- prompt_version。

前端可视化：

- 任务状态时间线。
- 工具调用链路。
- 模型调用日志。
- 报告证据来源。
- 审批记录。

## 18. 前端页面设计

核心页面：

- 登录页。
- 店铺选择页。
- Agent 对话/任务创建页。
- 任务看板。
- 任务详情页。
- 工具调用日志页。
- 报告详情页。
- 审批中心。
- 工具管理页。
- Prompt 管理页。

任务详情页展示：

- 用户原始输入。
- Agent 执行计划。
- 每一步工具调用。
- 中间结果。
- 最终报告。
- 审批记录。
- 错误和降级信息。

## 19. 模拟数据设计

为了项目可落地，可以先使用模拟数据代替真实淘宝/京东接口。

数据表：

- orders.csv：订单数据。
- products.csv：商品数据。
- comments.csv：评论数据。
- ads.csv：投放数据。
- keywords.csv：关键词数据。
- title_rules.md：标题规则。

模拟数据规模建议：

- 订单：5000 条。
- 商品：200 个。
- 评论：1000 条。
- 投放计划：100 条。
- 关键词：1000 条。

这样既能演示业务流程，又不会被真实开放平台权限卡住。

## 20. 评测方案

评测集：

- 日报复盘任务 30 条。
- 差评处理任务 20 条。
- 标题优化任务 20 条。
- 投放复盘任务 20 条。
- 权限/审批测试任务 30 条。

指标：

- 工具选择准确率。
- 工具调用成功率。
- 任务完成率。
- 报告结构完整率。
- 异常指标召回率。
- 差评分类准确率。
- 标题规则通过率。
- 平均任务耗时。
- 高风险操作拦截率。

可暂定目标：

- 工具调用成功率达到 95% 以上。
- 高风险操作拦截率 100%。
- 日报生成耗时控制在 5 分钟内。
- 异常指标召回率达到 85% 以上。

## 21. 实现路线图

### 阶段一：平台后端骨架

- Spring Boot 项目搭建。
- MySQL 表结构。
- 用户登录与权限。
- 店铺管理。
- 任务创建与查询。

### 阶段二：工具注册与模拟工具

- 工具注册表。
- 工具调用网关。
- 订单、评论、商品、投放模拟工具。
- 工具调用日志。

### 阶段三：Agent 编排

- Model Gateway。
- Ollama 接入。
- Planner。
- Executor。
- Verifier。
- 报告生成。

### 阶段四：异步任务与审批

- RabbitMQ 接入。
- Worker 执行。
- 任务状态机。
- 人工审批。
- 失败重试。

### 阶段五：前端工作台

- 任务创建页。
- 任务看板。
- 调用链详情。
- 报告详情。
- 审批中心。

### 阶段六：评测与优化

- 构建业务评测集。
- 统计任务成功率。
- 优化 Prompt。
- 优化工具路由。
- 完善降级机制。

## 22. 简历表述建议

项目名称：

ShopOps Agent：基于 MCP 的电商运营智能体平台

简历描述：

```latex
\resumeItem{\textbf{背景：}面向中小电商团队日常运营中多平台切换、复盘耗时长、差评响应滞后和操作风险不可控的问题，设计平台级电商运营 Agent 系统，统一承载店铺管理、工具注册、任务调度、审批流、审计日志和运营报告能力。}

\resumeItem{基于 \textbf{Spring Boot + MySQL + Redis + React} 构建平台后端与运营工作台，设计用户、店铺、角色权限、Agent 任务、工具调用、审批记录和报告管理等核心模块，支持运营任务创建、执行状态追踪、结果查看和人工确认。}

\resumeItem{围绕 MCP 工具编排思想，将订单查询、评论舆情、商品标题优化、投放复盘、Excel 导出和飞书同步封装为 \textbf{18 个标准化工具}，通过 \textbf{Planner-Executor-Verifier} 链路完成自然语言任务拆解、工具路由、结果聚合和风险校验。}

\resumeItem{设计 \textbf{异步任务队列、失败重试、权限校验、人工审批和全链路审计机制}，在 \textbf{120 个模拟运营任务}中完成 \textbf{462 次工具调用}，工具调用成功率达到 \textbf{96.8\%}，高风险操作均进入审批流程。}
```

## 23. 面试可讲亮点

可以重点准备以下问题：

1. 为什么把 ShopOps 做成平台，而不是单 Agent？
2. MCP 工具注册中心怎么设计？
3. Agent 如何选择工具？
4. 为什么需要人工审批？
5. 如何保证 Agent 不能越权调用工具？
6. 异步任务状态机怎么设计？
7. 工具失败和模型失败如何降级？
8. Ollama 本地模型如何接入？
9. Prompt 如何版本管理？
10. 调用链日志如何帮助排查问题？

## 24. 项目边界

第一版不强求真实接入淘宝/京东开放平台，可以使用模拟数据和本地工具完成完整流程。

第一版不做自动修改真实商品和投放配置，只做建议生成和审批模拟。

第一版重点是平台架构、工具编排、任务闭环和审计治理，而不是追求模型效果极限。

这种边界更适合学生项目，也更容易在面试中讲清楚。

