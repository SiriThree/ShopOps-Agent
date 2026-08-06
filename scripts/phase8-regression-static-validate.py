#!/usr/bin/env python3
"""Static guardrails for the Phase 8 full-regression repair.

This script does not replace Maven compilation or tests. It only proves structural
properties that can be checked without resolving Java dependencies.
"""
from __future__ import annotations

import hashlib
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks: list[tuple[str, bool, str]] = []


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def add(name: str, condition: bool, detail: str = "") -> None:
    checks.append((name, bool(condition), detail))


# Maven descriptors remain well-formed.
for pom in [
    "pom.xml",
    "shopops-common/pom.xml",
    "shopops-commerce-mcp-server/pom.xml",
    "shopops-admin/pom.xml",
]:
    try:
        ET.parse(ROOT / pom)
        add(f"POM parses: {pom}", True)
    except Exception as exc:  # pragma: no cover - command-line evidence
        add(f"POM parses: {pom}", False, str(exc))

catalog = text("shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/InMemoryMcpToolService.java")
app_yml = text("shopops-admin/src/main/resources/application.yml")
write_service = text("shopops-admin/src/main/java/com/sirithree/shopops/admin/reliability/service/WriteOperationService.java")
fake_client_path = ROOT / "shopops-admin/src/test/java/com/sirithree/shopops/admin/mcp/support/InMemoryCommerceMcpClient.java"
fake_client = fake_client_path.read_text(encoding="utf-8")
test_config = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/AgentIntegrationTestInfrastructure.java")
base_test = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/AbstractAgentTaskFlowIntegrationTest.java")
new_agent_test = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/agent/AgentMcpTestInfrastructureIntegrationTest.java")
new_write_test = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/reliability/service/WriteOperationServiceMemoryModeTest.java")
new_schema_test = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/mcp/support/InMemoryCommerceMcpClientTest.java")

add("Memory catalog keeps comment.query_negative as MCP", "setProviderType(CommerceMcpContracts.PROVIDER_MCP)" in catalog)
add("Production MCP remains disabled by default", "SHOPOPS_COMMERCE_MCP_ENABLED:false" in app_yml)
add("Test MCP client exists only in test scope", fake_client_path.is_file() and "/src/test/" in fake_client_path.as_posix())

production_mentions = []
for path in (ROOT / "shopops-admin/src/main/java").rglob("*.java"):
    if "InMemoryCommerceMcpClient" in path.read_text(encoding="utf-8"):
        production_mentions.append(path.relative_to(ROOT).as_posix())
add("Production source does not reference the test MCP client", not production_mentions, ", ".join(production_mentions))

add("Test client implements production CommerceMcpClient", "implements CommerceMcpClient" in fake_client)
add("Test client simulates discovery", "McpDiscoveryResult discover(" in fake_client and "discoveryCalls.incrementAndGet()" in fake_client)
add("Test client validates schema hash before call", fake_client.index("expectedSchemaHash.equals") < fake_client.index("call(context"))
add("Test client delegates business data to CommentRiskService", "commentRiskService.queryNegativeComments" in fake_client)
add("Test client validates trusted tenant/shop/user/trace context", all(token in fake_client for token in ["getTenantId()", "getShopId()", "getUserId()", "getTraceId()"]))
add("Test configuration registers a Primary client", "@Primary" in test_config and "InMemoryCommerceMcpClient" in test_config)
add("Agent integration base loads production app plus test infrastructure", "ShopOpsAdminApplication.class" in base_test and "AgentIntegrationTestInfrastructure.class" in base_test)
add("Daily Review test asserts SUCCESS through MCP provider", 'isEqualTo("SUCCESS")' in new_agent_test and "toolCallCount()).isEqualTo(1)" in new_agent_test)
add("Schema drift test asserts zero tools/call", "MCP_TOOL_SCHEMA_MISMATCH" in new_schema_test and "toolCallCount()).isZero()" in new_schema_test)

add("WriteOperationService no longer has method-level @Transactional", "@Transactional" not in write_service)
add("WriteOperationService injects transaction manager lazily", "ObjectProvider<PlatformTransactionManager>" in write_service)
add("Memory mode bypasses TransactionTemplate", "if (!jdbcPersistence)" in write_service and "return prepareMemory" in write_service)
add("JDBC mode retains explicit transaction boundary", "inJdbcTransaction" in write_service and "TransactionTemplate" in write_service)
add("Memory write test asserts no transaction/JDBC interactions", "verifyNoInteractions(transactionManagers, mapper, outbox)" in new_write_test)

# Default external tests must remain explicit rather than hidden default-suite dependencies.
external_test = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/mcp/OfficialCommerceMcpClientExternalIntegrationTest.java")
drift_external_test = text("shopops-admin/src/test/java/com/sirithree/shopops/admin/mcp/McpSchemaDriftExternalIntegrationTest.java")
add("External MCP success test remains opt-in", "EnabledIfSystemProperty" in external_test and "shopops.mcp.integration.enabled" in external_test)
add("External schema-drift test remains opt-in", "EnabledIfSystemProperty" in drift_external_test and "shopops.mcp.integration.enabled" in drift_external_test)

# Evaluation resources are present and no generated historical target artifacts were mistaken for source truth.
eval_dir = ROOT / "shopops-admin/src/test/resources/evaluation"
add("All evaluation datasets remain present", all((eval_dir / name).is_file() for name in [
    "agent-cases-v1.json", "agent-cases-model-v1.json", "agent-cases-degraded-v1.json"
]))
target_evidence = list(ROOT.glob("**/target/evaluation/*")) + list(ROOT.glob("**/target/surefire-reports/*"))
add("Repository contains no stale target evaluation/Surefire evidence", not target_evidence, f"found={len(target_evidence)}")

passed = 0
for name, ok, detail in checks:
    status = "PASS" if ok else "FAIL"
    print(f"[{status}] {name}" + (f" :: {detail}" if detail else ""))
    passed += int(ok)

failed = len(checks) - passed
print(f"TOTAL={len(checks)} PASS={passed} FAIL={failed}")
sys.exit(1 if failed else 0)
