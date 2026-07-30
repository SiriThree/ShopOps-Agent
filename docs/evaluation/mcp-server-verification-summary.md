# ShopOps MCP Server Verification

| Metric | Value |
| --- | --- |
| Endpoint | /mcp |
| Transport | HTTP JSON-RPC |
| Protocol versions | 2026-07-28 |
| Tool count | 18 |
| Passed checks | 4 / 4 |
| Success rate | 100% |
| Average latency | 131.8 ms |

## Checks

| Check | Result | Detail |
| --- | --- | --- |
| initialize | PASS | protocol=2026-07-28, latency=327.44ms |
| tools/list | PASS | toolCount=18, latency=50.72ms |
| tools/call read-only tool | PASS | tool=order.query_summary, status=SUCCESS, latency=140.08ms |
| tools/call high-risk approval path | PASS | tool=ad.suggest_budget, status=APPROVAL_REQUIRED, latency=8.94ms |

## Boundary

This verifies the Spring Boot embedded HTTP JSON-RPC MCP endpoint. It does not verify stdio or SSE transport.
