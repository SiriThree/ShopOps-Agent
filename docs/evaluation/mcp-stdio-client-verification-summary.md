# ShopOps stdio MCP Client Verification

| Metric | Value |
| --- | --- |
| Transport | stdio |
| Requests | 4 |
| Responses | 4 |
| Tool count | 18 |
| Passed checks | 5 / 5 |
| Success rate | 100% |
| Duration | 14670.18 ms |

## Checks

| Check | Result | Detail |
| --- | --- | --- |
| stdio process exited | PASS | completed=True, exitCode=0 |
| initialize response | PASS | protocol=2026-07-28 |
| tools/list response | PASS | toolCount=18 |
| read-only tools/call | PASS | status=SUCCESS |
| high-risk approval response | PASS | status=APPROVAL_REQUIRED |

## Boundary

This simulates an external MCP client launching ShopOps over stdio and exchanging line-delimited JSON-RPC messages.
