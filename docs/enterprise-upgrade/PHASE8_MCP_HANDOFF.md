# Phase 8 MCP Handoff — Read-only First Batch

## Delivered boundary

This handoff contains the first-batch `comment.query_negative` source implementation only. It is not the final Phase 8 handoff.

## Build changes

- Root module added: `shopops-commerce-mcp-server`.
- Official MCP Java SDK BOM pinned to `2.0.0`.
- Admin and Server use `mcp-core` plus `mcp-json-jackson2`.
- New independent Spring Boot Server jar.

## Database migration

`shopops-admin/src/main/resources/db/migration/V22__phase8_readonly_mcp.sql`

- creates `mcp_server`;
- adds provider/server/remote/schema/discovery columns to `mcp_tool`;
- registers `commerce-default`;
- binds only `comment.query_negative` to MCP;
- does not change its local permission/risk/approval/idempotency/enabled policy.

## Environment variables

Server:

```text
SHOPOPS_COMMERCE_MCP_PORT=8090
SHOPOPS_COMMERCE_MCP_TOKEN=<required>
```

Admin:

```text
SHOPOPS_COMMERCE_MCP_ENABLED=true
SHOPOPS_COMMERCE_MCP_SERVER_CODE=commerce-default
SHOPOPS_COMMERCE_MCP_BASE_URL=http://localhost:8090
SHOPOPS_COMMERCE_MCP_ENDPOINT=/mcp
SHOPOPS_COMMERCE_MCP_TOKEN=<same secret>
SHOPOPS_COMMERCE_MCP_CONNECT_TIMEOUT_MS=3000
SHOPOPS_COMMERCE_MCP_REQUEST_TIMEOUT_MS=5000
```

## Intended verification commands

Full first-batch tests:

```bash
mvn -pl shopops-commerce-mcp-server,shopops-admin -am test
```

Independent-process smoke test:

```bash
SHOPOPS_COMMERCE_MCP_TOKEN=phase8-test-token \
  ./scripts/phase8-mcp-readonly-smoke.sh
```

Static checks that were actually run:

```bash
python scripts/phase8-static-validate.py
```

## Actual result in the delivery environment

- Java: OpenJDK 21.0.10 available.
- Maven: unavailable (`command not found`, exit 127).
- Maven Wrapper: absent from the uploaded repository.
- Docker: unavailable.
- Network download: DNS blocked.
- Static checks: 21 passed, 0 failed.
- Java compile/test: not run successfully.
- Live `initialize/tools/list/tools/call`: test code exists but was not executed here.

Raw logs are included alongside the delivery archive.

## Demo after Maven is available

1. Set a non-empty MCP token.
2. Run `scripts/phase8-mcp-readonly-smoke.sh`.
3. Confirm the test suite passes.
4. Confirm Server log contains protocol records for `initialize`, `tools/list`, and exactly one `tools/call` across the normal and schema-drift tests.
5. Confirm the normal result has `negativeCount=3` and tenant/shop scope `1/1`.

## Remaining work before write tools

1. Resolve any compilation/API errors revealed by Maven in a network-enabled environment.
2. Run and archive the live protocol tests.
3. Persist discovery refresh state and add Admin registration/refresh APIs.
4. Add immutable `ExecutionSnapshot` with schema/input hash binding.
5. Only then add `product.query_candidates`.
6. Add `product.update_title` and `order.refund_execute` with idempotency, unknown-result semantics, reconciliation, duplicate callback/call tests, and approval-before-call count assertions.
7. Add Docker Compose wiring and health dependency status.

## Acceptance statement

The source implementation for the first read-only protocol slice is delivered. Phase 8 is not marked complete, and the first batch is not marked runtime-accepted until Maven compilation and the live protocol tests pass.
