#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${SHOPOPS_COMMERCE_MCP_PORT:-18090}"
TOKEN="${SHOPOPS_COMMERCE_MCP_TOKEN:-phase8-test-token}"
LOG_DIR="$ROOT_DIR/artifacts/phase8"
SERVER_LOG="$LOG_DIR/commerce-mcp-server.log"
mkdir -p "$LOG_DIR"

cd "$ROOT_DIR"
mvn -pl shopops-commerce-mcp-server,shopops-admin -am -DskipTests package

SHOPOPS_COMMERCE_MCP_PORT="$PORT" \
SHOPOPS_COMMERCE_MCP_TOKEN="$TOKEN" \
java -jar shopops-commerce-mcp-server/target/shopops-commerce-mcp-server-0.1.0-SNAPSHOT.jar \
  >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!
trap 'kill "$SERVER_PID" >/dev/null 2>&1 || true' EXIT

for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${PORT}/actuator/health" >/dev/null; then
    break
  fi
  sleep 1
done
curl -fsS "http://127.0.0.1:${PORT}/actuator/health" >/dev/null

mvn -pl shopops-admin -am \
  -Dtest=OfficialCommerceMcpClientExternalIntegrationTest,McpSchemaDriftExternalIntegrationTest \
  -Dshopops.mcp.integration.enabled=true \
  -Dshopops.mcp.integration.base-url="http://127.0.0.1:${PORT}" \
  -Dshopops.mcp.integration.token="$TOKEN" \
  test

INITIALIZE_COUNT="$(grep -cE '\[MCP-PROTOCOL\].*method=initialize([[:space:]]|$)' "$SERVER_LOG" || true)"
LIST_COUNT="$(grep -cE '\[MCP-PROTOCOL\].*method=tools/list([[:space:]]|$)' "$SERVER_LOG" || true)"
CALL_COUNT="$(grep -cE '\[MCP-PROTOCOL\].*method=tools/call([[:space:]]|$)' "$SERVER_LOG" || true)"

printf 'initialize=%s\ntools/list=%s\ntools/call=%s\n' \
  "$INITIALIZE_COUNT" "$LIST_COUNT" "$CALL_COUNT"

test "$INITIALIZE_COUNT" -ge 2
test "$LIST_COUNT" -ge 2
# The normal gateway test calls once; schema-drift test must reject before tools/call.
test "$CALL_COUNT" -eq 1

grep -E '\[MCP-PROTOCOL\].*method=(initialize|tools/list|tools/call)' "$SERVER_LOG"
