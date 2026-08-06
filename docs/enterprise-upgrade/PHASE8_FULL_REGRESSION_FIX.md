# ShopOps Phase 8 全量回归修复报告

## 1. 结论与证据边界

本轮以当前仓库源码和测试为事实来源完成了两组最小修复：

1. 为默认 Agent 集成测试提供 test-scope MCP Client 基础设施，保持 `comment.query_negative` 的 `providerType=MCP`，继续经过 `DefaultToolGatewayService`、`McpToolProvider`、Schema Hash 校验和真实业务查询服务，但不要求开发者预先启动独立 MCP Server。
2. 修复 `WriteOperationService` 在 `shopops.persistence=memory` 时仍被 Spring JDBC 事务代理提前打开连接的问题；内存路径现在完全绕过事务管理器，JDBC 路径继续使用显式 `TransactionTemplate` 保持事务边界。

当前 ZIP 未包含：

```text
shopops-admin/target/evaluation/
shopops-admin/target/surefire-reports/
```

因此，提示词中历史 `126 tests / 10 failures` 只能作为输入基线，无法从当前 ZIP 重新读取旧的逐 Case Artifact 或 Surefire XML。本文的第一失败点来自测试源码、生产调用链和历史汇总的交叉核对；最终通过状态仍必须由 Maven 重跑确认。

当前执行环境没有 `mvn`，仓库也没有 Maven Wrapper，所有 Maven 命令均真实执行但以退出码 `127` 失败。因此本轮**不能宣称 BUILD SUCCESS，也不能宣称 10 个失败已经运行验证通过**。

---

## 2. 真实根因

### 2.1 Daily Review / Model Evaluation 进入 DEGRADED

内存工具目录仍将 `comment.query_negative` 注册为：

```text
providerType=MCP
mcpServerCode=commerce-default
discoveryStatus=READY
```

生产默认配置同时是：

```text
SHOPOPS_COMMERCE_MCP_ENABLED=false
SHOPOPS_COMMERCE_MCP_TOKEN=
```

原默认测试只配置：

```text
shopops.persistence=memory
```

并没有启动独立 MCP Server，也没有注入测试 MCP Client。真实失败链如下：

```text
Daily Review
→ SequentialAgentExecutorService
→ DefaultToolGatewayService
→ providerType=MCP
→ McpToolProvider
→ OfficialCommerceMcpClient
→ commerce server disabled / unavailable
→ comment.query_negative FAILED
→ 非关键步骤失败
→ executionResult.degraded=true
→ Task 最终状态 DEGRADED
```

`SequentialAgentExecutorService` 对 `comment.query_negative` 失败不会立即中止，而是设置 `degraded=true` 后继续生成报告，因此：

- 明确断言 `status=SUCCESS` 的测试失败；
- 只读取报告中的订单指标、但不检查状态的测试可能仍通过；
- Evaluation 会同时因 `finalStatusIn`、`expectDegraded=false`、工具结果或 Evidence 不完整而计为失败。

这也解释了历史结果中普通 Evaluation 仅 1/7 全部满足、Model Evaluation 0/4 全部满足，而不是数据集只加载了 1 条或 0 条。

### 2.2 内存退款仍尝试 JDBC 连接

原 `WriteOperationService` 的四个 public 方法都带有：

```java
@Transactional
public WriteOperation prepare(...) {
    if (!jdbcPersistence) {
        return prepareMemory(...);
    }
    ...
}
```

Spring 事务拦截器会在进入方法体之前创建事务，因此 `if (!jdbcPersistence)` 来不及阻止 `DataSourceTransactionManager` 获取 JDBC Connection。真实失败链是：

```text
Tool Gateway
→ HighRiskRefundExecuteExecutor
→ WriteOperationService.prepare(...)
→ Spring @Transactional interceptor
→ DataSourceTransactionManager.getTransaction(...)
→ 尝试打开 JDBC Connection
→ 等待连接超时（历史日志约 30 秒）
→ Could not open JDBC Connection for transaction
→ ToolInvokeResult success=false
```

问题不在 `RefundExternalClient`。当前仓库中的退款 Connector 已是本地确定性模拟器；失败发生在进入业务方法之前的事务基础设施边界。

---

## 3. 历史 10 个失败的失败矩阵

> `actualStatus` 和聚合计数来自任务输入中的历史回归结果；旧 Surefire XML 与 Evaluation Artifact 未包含在当前 ZIP 中。第一失败步骤和根因由当前源码调用链确认。

| # | 测试 / 场景 | 历史实际 | 第一失败点 | 直接原因 | 根因组 | 修复 |
|---|---|---|---|---|---|---|
| 1 | `AgentEvaluationIntegrationTest#shouldRunAgentEvaluationCasesAndWriteSummaryArtifacts` | 7 Case 仅 1 条通过 | Evaluation 最终 `passedCaseCount` | Daily Review 为 DEGRADED；审批关闭退款失败 | MCP + JDBC | 测试 MCP Client + 事务边界修复 |
| 2 | `AgentEvaluationModelIntegrationTest#shouldRunAgentEvaluationCasesAndWriteSummaryArtifacts` | 4 Case 0 条通过 | Evaluation 最终 `passedCaseCount` | 报告模型执行前，Daily Review 已因 MCP 步骤降级 | MCP | 测试 MCP Client |
| 3 | `AgentNaturalLanguageTaskIntegrationTest#shouldCreateDailyReviewTaskFromNaturalLanguage` | expected SUCCESS, actual DEGRADED | `task.status` | `comment.query_negative` 远程 Client 不可用 | MCP | 测试 MCP Client |
| 4 | `AgentNaturalLanguageTaskIntegrationTest#shouldClassifySpecializedNaturalLanguageIntent` | expected SUCCESS, actual DEGRADED | `task.status` | 专项计划仍包含 MCP 差评查询 | MCP | 测试 MCP Client |
| 5 | `AgentNaturalLanguageTaskIntegrationTest#shouldGenerateOlistDailyReviewFromDefaultDemoConnectors` | expected SUCCESS, actual DEGRADED | `task.status` | Olist 差评数据步骤被错误地绑定到不可用外部进程 | MCP | 测试 MCP Client 复用 `CommentRiskService` |
| 6 | `AgentTaskMemoryFlowIntegrationTest#shouldCreateDailyReviewTaskAndPersistReportInMemoryMode` | expected SUCCESS, actual DEGRADED | 创建结果状态断言 | MCP 只读步骤失败 | MCP | 测试 MCP Client |
| 7 | `AgentTaskModelPlannerIntegrationTest#shouldCreateDailyReviewTaskWithModelGatewayPlannerWhenEnabled` | expected SUCCESS, actual DEGRADED | 创建结果状态断言 | 模型 Planner 正常，执行阶段 MCP 步骤失败 | MCP | 测试 MCP Client |
| 8 | `AgentTaskModelReportIntegrationTest#shouldGenerateDailyReviewReportThroughModelGatewayWhenEnabled` | expected SUCCESS, actual DEGRADED | 创建结果状态断言 | Echo Report Provider 前置工具链已降级 | MCP | 测试 MCP Client |
| 9 | `ToolApprovalGatewayIntegrationTest#shouldCreateApprovalRequestWhenToolRequiresApproval` | retry expected success=true, actual false | 审批通过后的第二次工具调用 | 进入写操作时事务代理打开 JDBC | JDBC | 内存路径绕过事务管理器 |
| 10 | `ToolApprovalGatewayIntegrationTest#shouldBypassApprovalWhenShopConfigDisablesToolApproval` | expected success=true, actual false | 审批绕过后的退款执行 | 同上 | JDBC | 内存路径绕过事务管理器 |

`shouldGenerateDifferentOlistDailyReviewsForDifferentCoveredDates` 没有直接断言 Task 状态，只读取仍生成的报告中订单指标，因此它可以在任务 DEGRADED 时继续通过；`shouldRoutePortfolioDemoPrompts` 只验证路由结果。这与历史总失败数 10 一致。

---

## 4. MCP 测试基础设施设计

### 4.1 生产环境

```text
DefaultToolGatewayService
→ McpToolProvider
→ OfficialCommerceMcpClient
→ initialize
→ tools/list
→ schema hash check
→ tools/call
→ 独立 Commerce MCP Server
```

生产默认仍不启用 MCP；启用时必须提供 endpoint 和 token。没有增加 Local fallback，也没有改变 `InMemoryMcpToolService` 中的 `providerType=MCP`。

### 4.2 默认测试环境

```text
DefaultToolGatewayService
→ McpToolProvider
→ @Primary InMemoryCommerceMcpClient（仅 src/test）
→ discover / schema hash check / call
→ CommentRiskService
→ File/Default Commerce test data
```

关键性质：

- Fake 位于 `src/test/java`，不进入生产包；
- 只替换 `CommerceMcpClient` 基础设施边界，不替换 `ToolGatewayService` 或 `McpToolProvider`；
- 工具元数据继续是 `providerType=MCP`；
- 保留 discovery、远程工具名、Schema Hash、trusted tenant/shop/user/trace 和结构化返回；
- Schema Drift 在调用业务服务前拒绝，`toolCallCount=0`；
- 返回结构来自现有 `CommentRiskService`，不是固定 `success=true`；
- 生产代码没有测试分支或自动 Local fallback。

外部双 JVM MCP 测试继续由 `shopops.mcp.integration.enabled=true` 显式开启，不成为默认 `mvn clean test` 对手工进程的隐式依赖。

---

## 5. 退款事务修复

### 修改前

```text
@Transactional public method
→ transaction interceptor always runs
→ method body checks memory too late
```

### 修改后

```text
public method
├─ memory → in-memory state machine, no transaction-manager access
└─ jdbc   → TransactionTemplate.execute(...)
             ├─ prepare + transition
             ├─ external result + local confirmation
             └─ outbox write
```

JDBC 逻辑仍在一个显式事务中；内存模式不会请求 `PlatformTransactionManager`，也不会访问 Mapper 或 Outbox。重复 idempotency key 的 JDBC 竞争路径还补充了 input hash 一致性校验，避免重复键发生后返回不同参数的已有操作。

---

## 6. 修改文件

### 生产代码

#### `shopops-admin/.../reliability/service/WriteOperationService.java`

- 问题：方法级 `@Transactional` 在 memory 分支前打开 JDBC。
- 修改：按 persistence mode 显式选择 memory 路径或 `TransactionTemplate`。
- 生产影响：memory 模式不再依赖 JDBC；jdbc 模式保留事务与 Outbox 原子性。
- 对应测试：`WriteOperationServiceMemoryModeTest`、`ToolApprovalGatewayIntegrationTest`、退款 Evaluation Case。

### 测试基础设施

#### `shopops-admin/src/test/.../mcp/support/InMemoryCommerceMcpClient.java`

- test-scope `CommerceMcpClient`；模拟 discovery/list/call 语义、Schema Hash 与错误分类；调用现有业务数据服务。

#### `shopops-admin/src/test/.../agent/AgentIntegrationTestInfrastructure.java`

- 注册 `@Primary InMemoryCommerceMcpClient`，仅用于继承 Agent 测试基类的测试上下文。

#### `shopops-admin/src/test/.../agent/AbstractAgentTaskFlowIntegrationTest.java`

- 在生产 `ShopOpsAdminApplication` 基础上追加测试基础设施，不建立平行 Spring 应用。

### 新增测试

#### `AgentMcpTestInfrastructureIntegrationTest`

- 断言完整 Daily Review 为 SUCCESS；
- 断言 MCP discovery 一次、tools call 一次。

#### `InMemoryCommerceMcpClientTest`

- 断言 Schema Hash 被篡改时抛出 `MCP_TOOL_SCHEMA_MISMATCH`；
- 断言 discovery 一次、tools call 为零、业务服务未触达。

#### `WriteOperationServiceMemoryModeTest`

- 驱动 APPROVED → EXECUTING → EXTERNAL_SUCCEEDED → LOCAL_CONFIRMED → SUCCEEDED；
- 断言 transaction manager、JDBC mapper、outbox 均未被访问。

### 验证脚本

#### `scripts/phase8-regression-static-validate.py`

- 只做结构性防回归，不替代 Maven；
- 检查 test fake 隔离、MCP provider 语义、Schema Drift、事务边界、外部测试 opt-in 和 Evaluation 数据存在性。

未修改 Evaluation JSON、既有断言、生产 MCP Server 或官方 MCP Client 错误映射。

---

## 7. 静态验证结果

实际命令：

```bash
python scripts/phase8-static-validate.py
python scripts/phase8-regression-static-validate.py
```

结果：

```text
Phase 8 MCP structural checks: TOTAL=21 PASS=21 FAIL=0
Regression repair checks:      TOTAL=26 PASS=26 FAIL=0
```

静态验证不能证明 Java 编译、Spring Context 启动或测试通过。

---

## 8. Maven 命令真实结果

执行环境：Java 可用，但 `mvn` 不存在，仓库没有 `mvnw`。

以下命令均已实际发起：

```text
mvn -pl shopops-admin -am test-compile
mvn -pl shopops-admin test -Dtest=AgentNaturalLanguageTaskIntegrationTest#shouldCreateDailyReviewTaskFromNaturalLanguage -Dsurefire.useFile=false
mvn -pl shopops-admin test -Dtest=ToolApprovalGatewayIntegrationTest -Dsurefire.useFile=false
mvn -pl shopops-admin test -Dtest=AgentEvaluationIntegrationTest,AgentEvaluationModelIntegrationTest -Dsurefire.useFile=false
mvn -pl shopops-admin -am test -Dtest=McpServerUnavailableIntegrationTest,McpToolGatewayGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl shopops-admin test -Dtest=AgentMcpTestInfrastructureIntegrationTest,InMemoryCommerceMcpClientTest,WriteOperationServiceMemoryModeTest -Dsurefire.useFile=false
mvn clean test
mvn clean package -DskipTests
```

每条命令的真实结果均为：

```text
mvn: command not found
EXIT_CODE: 127
```

因此没有 `Tests run / Failures / Errors / Skipped` 或 `BUILD SUCCESS` 可报告。

---

## 9. 待运行验证与剩余风险

在具备 Maven 3.9.x、JDK 17/21 和可用依赖缓存的环境中，必须按以下顺序运行：

1. `mvn -pl shopops-admin -am test-compile`
2. 三个新增针对性测试
3. 单条 Daily Review
4. `ToolApprovalGatewayIntegrationTest`
5. 普通与 Model Evaluation
6. MCP 不可达与 Gateway Governance 回归
7. `mvn clean test`
8. `mvn clean package -DskipTests`

当前尚未排除的运行期风险：

- Spring TestContext 合并生产应用、嵌套 TestConfiguration 和共享测试 MCP 配置时的实际 Bean 装配；
- 官方依赖解析后的 Java 编译签名；
- 全量测试中其他未出现在历史 10 个失败里的隐藏回归；
- 多测试类上下文缓存下的内存状态污染；
- JDBC 模式现有测试对显式 `TransactionTemplate` 的真实回归。

只有上述 Maven 验证全部成功后，才能将本轮标记为“全量回归完成”。
