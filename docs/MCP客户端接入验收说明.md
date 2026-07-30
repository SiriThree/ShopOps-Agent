# MCP 客户端接入验收说明

本文用于说明 ShopOps 标准 MCP Server 的外部客户端接入方式，以及当前已经复跑通过的验收边界。

## 当前能力

ShopOps 已提供三种 MCP transport：

| Transport | 入口 | 用途 |
| --- | --- | --- |
| HTTP JSON-RPC | `POST /mcp` | 后端服务内嵌 MCP endpoint，适合脚本、网关或 HTTP 客户端验证 |
| stdio | `scripts/run-mcp-stdio-server.ps1` | 独立 stdio MCP Server 进程，适合 MCP Client 直接拉起 |
| SSE | `GET /mcp/sse` + `POST /mcp/messages?sessionId=...` | HTTP SSE 会话模式，适合需要长连接消息推送的客户端 |

支持的协议方法：

- `server/discover`
- `initialize`
- `tools/list`
- `tools/call`

`tools/call` 复用 ShopOps 现有 `ToolGatewayService`，因此工具权限、调用日志、人工审批、店铺配置和审计链路都会继续生效。

## stdio MCP Client 配置

通用 MCP Client 可以按下面方式配置 ShopOps：

```json
{
  "mcpServers": {
    "shopops": {
      "command": "powershell",
      "args": [
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "D:\\找实习\\ShopOps\\scripts\\run-mcp-stdio-server.ps1"
      ]
    }
  }
}
```

说明：

- `run-mcp-stdio-server.ps1` 默认使用 `memory` 模式启动，便于外部 MCP Client 直接拉起。
- 后台管理端仍可通过 `scripts/start-shopops.ps1` 使用 JDBC/MySQL 持久化模式。
- stdio 进程默认请求上下文为租户 `1`、店铺 `1`、用户 `1`、角色 `ADMIN`。
- 如需覆盖上下文，可设置环境变量 `SHOPOPS_MCP_TENANT_ID`、`SHOPOPS_MCP_SHOP_ID`、`SHOPOPS_MCP_USER_ID`、`SHOPOPS_MCP_USERNAME`、`SHOPOPS_MCP_ROLES`。

## 一键验收

复跑 stdio MCP Client 验收：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-mcp-stdio-client.ps1 -SkipCompile
```

该脚本会模拟外部 MCP Client：

1. 启动 ShopOps stdio MCP Server 进程。
2. 发送 line-delimited JSON-RPC 请求。
3. 验证 `initialize` 返回协议版本。
4. 验证 `tools/list` 至少返回 18 个工具。
5. 验证只读工具 `order.query_summary` 调用成功。
6. 验证高风险工具 `ad.suggest_budget` 返回 `APPROVAL_REQUIRED`。

当前验收产物：

- `docs/evaluation/mcp-stdio-client-verification-summary.json`
- `docs/evaluation/mcp-stdio-client-verification-summary.md`

## 验收边界

当前证据证明：外部 MCP Client 可以通过 stdio 拉起 ShopOps，并完成标准 JSON-RPC 初始化、工具发现和工具调用。

HTTP JSON-RPC 的证据由 `scripts/verify-mcp-server.ps1` 生成；SSE transport 已通过后端集成测试覆盖。当前尚未声明已经在某个具体第三方 MCP Client UI 中完成点击式验收。
