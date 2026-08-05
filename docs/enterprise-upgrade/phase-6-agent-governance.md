# Phase 6 Agent Platform Governance Handoff

## 1. 阶段审计结论

当前 Agent 主流程真实存在：任务创建后由 Planner 生成计划，Plan Validator 校验，Sequential Executor 经 Tool Gateway 执行，Verifier 检查执行结果和证据，失败时最多进行一次补充证据式 Repair，随后生成报告并记录 Trace。Agent 并非直接访问数据库，工具权限、审批和审计主要由 Tool Gateway 裁决。

阶段开始前的主要缺口：

- 没有明确的 ADVISORY、DRAFT、AUTOMATIC 平台执行模式；
- Plan Validator 只检查步骤连续、工具存在、工具不重复和报告末步；
- 模型 Planner 虽然有固定工具列表，但该约束没有抽象为可复用运行时工作流模板；
- 工具启用状态、版本、权限、风险上限和执行模式没有在计划校验阶段统一检查；
- Repair 固定写在 Engine 中，缺少模板级修复预算；
- Repair Plan 在执行前没有重新经过 Plan Validator；
- Verifier 主要验证执行成功、报告存在和证据工具结果，不是完整的数据库/外部状态回查验证器；
- Agent 评测已有历史文档和接口，但本阶段未发现能够证明全部任务级治理指标真实执行通过的结果。

## 2. 实际完成范围

- 新增 `AgentExecutionMode`：`ADVISORY`、`DRAFT`、`AUTOMATIC`；
- `AgentTaskCreateParam` 增加 `executionMode`，安全默认值为 `ADVISORY`；
- 新增受控 `WorkflowTemplate` 与 `WorkflowTemplateRegistry`；
- 将现有 `daily_review`、`comment_risk`、`product_optimization`、`ad_anomaly` 注册为有限工作流模板；
- 模板真实定义允许工具、最大步骤、风险上限、允许模式、最大修复次数和任务超时元数据；
- 增强 Plan Validator，真实检查：
  - 工作流模板是否存在；
  - 执行模式是否合法；
  - 最大步骤数；
  - 工具是否属于模板；
  - 工具是否注册且启用；
  - 工具是否具有版本；
  - 当前主体是否拥有工具权限；
  - 工具风险是否超过模板上限；
  - AUTOMATIC 是否尝试规划高风险或需审批工具；
  - 报告工具是否严格位于末步；
- Repair Plan 在执行前重新经过同一 Plan Validator；
- Repair 次数改为模板治理，当前所有模板最多一次；
- Agent 上下文记录开始时间和修复次数，为后续 Trace/评测持久化提供运行时字段；
- 新增执行模式和工作流模板单元测试代码。

## 3. 未完成范围

- 执行模式尚未持久化到 `agent_task`、TraceSpan 和 Operation Report 数据表；
- 当前 Controller/API 尚未针对不同权限动态降低用户请求的执行模式；
- DRAFT 模式尚未形成专门的“只生成草稿不执行写操作”执行分支；
- 当前模板只覆盖已有日报及三个专项意图，没有新增差评退款、商品写入等写工作流模板；
- 工具元数据尚未增加 `readOnly`、`reversible`、`approvalPolicy`、`retryPolicy`、`allowedWorkflowTypes` 数据库字段；
- input/output Schema 尚未在 Plan Validator 中进行真实 JSON Schema 校验；
- 计划步骤没有显式依赖字段，因此循环依赖只能通过当前顺序计划模型间接避免；
- 总任务超时、模型 Token/成本预算尚未执行式接入；
- Tool Gateway 尚未用 `Future` 或 Resilience4j 强制单工具 timeout；
- Verifier 仍以工具结果和证据存在性为主，没有完成数据库状态、外部回查和报告数值重算；
- 修复原因记录在 Trace input summary 中，但尚无独立 repair_attempt 表；
- 没有在本环境真实运行 Agent 安全回归和任务级评测。

## 4. 修改文件清单

- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/domain/AgentTaskCreateParam.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/domain/AgentTaskContext.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/DefaultPlanValidator.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/DefaultAgentEngineService.java`
- `shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/service/impl/DefaultPlanValidatorTest.java`
- `shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/service/impl/DefaultAgentEngineServiceTest.java`

## 5. 新增文件清单

- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/domain/AgentExecutionMode.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/governance/WorkflowTemplate.java`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/governance/WorkflowTemplateRegistry.java`
- `shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/domain/AgentExecutionModeTest.java`
- `shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/governance/WorkflowTemplateRegistryTest.java`
- `docs/enterprise-upgrade/phase-6-agent-governance.md`

## 6. 删除文件清单

无。

## 7. 核心设计说明

平台先根据实际业务意图选择一个固定工作流模板。Planner 只能在模板允许范围内提出计划，Plan Validator 再以真实租户、店铺和用户身份检查工具权限、启用状态、版本、风险和执行模式。AUTOMATIC 不能规划需要审批或 HIGH/CRITICAL 工具。即使 Planner 或 Repair 生成了越界步骤，也会在 Executor 前被拒绝。Tool Gateway 仍会在真正执行时再次检查权限和审批，形成计划阶段与执行阶段双重治理。

当前 ADVISORY 为默认模式，避免调用方不显式指定模式时自动扩大执行权。由于现有计划主要由只读查询和报告生成组成，本阶段没有人为引入新的高风险写工具模板。

## 8. 数据库变化

无。执行模式、模板版本和修复次数目前只存在于运行时对象，尚未持久化。

## 9. 配置变化

无新增外部配置。模板当前采用代码注册，避免在缺少策略签名、版本和权限治理前允许普通配置动态扩大工具范围。

## 10. 已执行测试及结果

- `node --check performance/k6-smoke.js`：退出码 0，仅证明既有性能脚本 JavaScript 语法有效，与 Agent 治理功能正确性无关。
- 新增了 `AgentExecutionModeTest` 和 `WorkflowTemplateRegistryTest`，并更新既有 Validator/Engine 测试构造方式，但未能实际运行 Maven 测试。

## 11. 未能执行的验证

- `mvn test`：未执行，环境不存在 Maven，退出码 127；
- 前端 `npm run typecheck`：失败，当前目录未安装 React、Vite、Ant Design 等依赖；
- 未执行 MySQL/RabbitMQ 集成测试；
- 未执行模型生成不存在工具、跨店铺、HIGH 工具自动执行、Tool 超时、外部结果未知、修复耗尽等真实入口测试；
- 未执行任务级评测，因此没有新的准确率、完成率、延迟或成本指标。

## 12. 已知风险

- 新增 Java 代码尚未通过真实 Maven 编译，可能仍存在编译级问题；
- 只靠代码模板不能替代数据库策略版本、变更审批和发布治理；
- 工具数据库元数据仍不足以完整描述可逆性、重试和工作流兼容性；
- ADVISORY 当前主要是治理声明和默认值，Executor 尚未按模式阻断所有潜在写工具；
- Verifier 仍不足以证明外部写操作真实成功；
- Agent 任务统计还不能完整计算计划合法率、工具选择准确率、参数准确率和单任务成本。

## 13. 下一阶段依赖

- 在具备 Maven、MySQL、RabbitMQ 的环境执行完整编译和集成测试；
- 持久化 `execution_mode`、`workflow_type`、`workflow_version`、`repair_attempts`、预算使用量；
- 扩展工具注册表元数据并提供数据库迁移；
- 增加基于 JSON Schema 的输入输出验证；
- 将任务总超时、工具超时、模型 Token/成本预算接入执行链；
- 将 Verifier 扩展为数据库/外部状态回查与报告一致性验证；
- 建立隔离测试集和任务级指标计算器。

## 14. 简历声明边界

可以谨慎表述：

> 为 ShopOps Agent 引入 ADVISORY/DRAFT/AUTOMATIC 执行模式和受控工作流模板，增强 Plan Validator 对工具白名单、启用状态、版本、权限、风险和步骤预算的运行时校验，并将补证修复限制为模板定义的单次安全修复。

不能宣称：

- 已完成通用自主 Agent 平台；
- 所有执行模式均已完整持久化和端到端验证；
- 已完成 JSON Schema、循环依赖、Token/成本预算和 Tool timeout 全治理；
- Verifier 已对所有写操作完成数据库和外部回查；
- Agent 任务级指标已经在真实商家数据上通过评测。
