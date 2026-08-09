#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = {
    "productionTreeSha256": "15846b40317611e24d6eab1d00de5a235ff98373c69fd0b5bf9a28c82c4e953f",
    "productionFileCount": 530,
    "otherBenchmarkTreeSha256": "fad17293f0642dd126814956552299ae97dc9c2a0087b158e8584430138215f0",
    "otherBenchmarkFileCount": 23,
    "stage5ManifestSha256": "ba9b53cd8ce22f23afd3bd61ab86df0c689884b31375238d6b4783bb9c21a27d",
    "stage5WorkloadSha256": "970edc2ca45c5feeaefc09d9cd2af83f85cae5d32581beade9af40604c02e6c5",
    "idempotencyCaseFileSha256": {
        "dev": "70f7a150b18f0f360386aa7644e2a74f38dfeb50a39fad3f2ce4ddf4012e493b",
        "validation": "f885516aa5a16e0fc14f92b7741ed7fb41ecc76bfa687c55d53ff0dd095d1fd5",
        "test": "97f2365f6b8457d8a1c6d35aac0489936b944c31838b966aa1864c8abcc3ff5f",
    },
}

def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def tree_hash(paths: list[Path]) -> tuple[str, int]:
    rows = []
    for base in paths:
        for path in sorted(base.rglob("*")):
            if path.is_file():
                rows.append(f"{path.relative_to(ROOT).as_posix()}\0{sha(path)}")
    return hashlib.sha256("\n".join(rows).encode()).hexdigest(), len(rows)

def load_cases() -> tuple[list[dict], dict[str, list[dict]]]:
    by_split = {}
    all_cases = []
    for split in ("dev", "validation", "test"):
        path = ROOT / f"shopops-admin/src/test/resources/benchmark/v1/idempotency/{split}/cases.json"
        data = json.loads(path.read_text())
        by_split[split] = data
        all_cases.extend(data)
    return all_cases, by_split

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="artifacts/evaluation/stage6-idempotency-attribution-audit.json")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    checks = {}
    prod_hash, prod_count = tree_hash([
        ROOT / "shopops-admin/src/main",
        ROOT / "shopops-common/src/main",
        ROOT / "shopops-commerce-mcp-server/src/main",
    ])
    checks["productionIntegrity"] = {
        "actualSha256": prod_hash, "expectedSha256": EXPECTED["productionTreeSha256"],
        "fileCount": prod_count, "expectedFileCount": EXPECTED["productionFileCount"],
        "pass": prod_hash == EXPECTED["productionTreeSha256"] and prod_count == EXPECTED["productionFileCount"],
    }

    other_hash, other_count = tree_hash([
        ROOT / "shopops-admin/src/test/resources/benchmark/v1/task",
        ROOT / "shopops-admin/src/test/resources/benchmark/v1/governance",
        ROOT / "shopops-admin/src/test/resources/benchmark/v1/recovery",
    ])
    checks["otherBenchmarkIntegrity"] = {
        "actualSha256": other_hash, "expectedSha256": EXPECTED["otherBenchmarkTreeSha256"],
        "fileCount": other_count, "expectedFileCount": EXPECTED["otherBenchmarkFileCount"],
        "pass": other_hash == EXPECTED["otherBenchmarkTreeSha256"] and other_count == EXPECTED["otherBenchmarkFileCount"],
    }

    cases, by_split = load_cases()
    roots = {c.get("semanticRootId") for c in cases}
    split_roots = {s: {c.get("semanticRootId") for c in rows} for s, rows in by_split.items()}
    root_leaks = sorted((split_roots["dev"] & split_roots["validation"]) |
                        (split_roots["dev"] & split_roots["test"]) |
                        (split_roots["validation"] & split_roots["test"]))
    case_hashes = {}
    case_files_unchanged = True
    for split in ("dev", "validation", "test"):
        path = ROOT / f"shopops-admin/src/test/resources/benchmark/v1/idempotency/{split}/cases.json"
        actual = sha(path)
        expected = EXPECTED["idempotencyCaseFileSha256"][split]
        case_hashes[split] = {"actual": actual, "expected": expected, "pass": actual == expected}
        case_files_unchanged &= actual == expected
    checks["datasetUnchanged"] = {
        "caseCount": len(cases),
        "semanticScenarioCount": len(roots),
        "crossSplitRootLeakage": len(root_leaks),
        "rootLeakIds": root_leaks,
        "caseFiles": case_hashes,
        "pass": len(cases) == 22 and len(roots) == 16 and not root_leaks and case_files_unchanged,
    }

    stage5_manifest = ROOT / "shopops-admin/src/test/resources/benchmark/v1/benchmark-idempotency-stage5-candidate-manifest.json"
    stage5_workload = ROOT / "shopops-admin/src/test/resources/benchmark/v1/idempotency/stage5/idempotency-workload-profiles.json"
    checks["stage5ArtifactsUnchanged"] = {
        "manifestSha256": sha(stage5_manifest),
        "workloadSha256": sha(stage5_workload),
        "pass": sha(stage5_manifest) == EXPECTED["stage5ManifestSha256"] and sha(stage5_workload) == EXPECTED["stage5WorkloadSha256"],
    }

    write_service = (ROOT / "shopops-admin/src/main/java/com/sirithree/shopops/admin/reliability/service/WriteOperationService.java").read_text()
    gateway = (ROOT / "shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/service/impl/DefaultToolGatewayService.java").read_text()
    executor = (ROOT / "shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/idempotency/RefundIdempotencyBenchmarkExecutor.java").read_text()
    factory = (ROOT / "shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/idempotency/FreshReplayApprovalFactory.java").read_text()
    evaluator = (ROOT / "shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/idempotency/SideEffectIdempotencyEvaluator.java").read_text()
    formal = (ROOT / "shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/formal/FormalBenchmarkEligibility.java").read_text()

    checks["canonicalHashContract"] = {
        "writeOperationExcludesApprovalId": 'values.remove("approvalId")' in write_service,
        "gatewayApprovalBindingExcludesApprovalId": 'values.remove("approvalId")' in gateway,
        "pass": 'values.remove("approvalId")' in write_service and 'values.remove("approvalId")' in gateway,
    }
    checks["freshApprovalContract"] = {
        "factoryExists": bool(factory),
        "usesApprovalServiceApprove": "approvals.approve(" in factory,
        "usesGatewayApprovalRequiredPath": 'toolGateway.invoke(context, "order.refund_execute", input)' in factory,
        "directApprovalStatusMutation": "setStatus(" in factory,
        "driverCreatesFreshApprovalPerAttempt": "freshApproval(" in executor and "attemptApprovals.add(freshApproval" in executor,
        "oldSingleApprovalReusePatternPresent": "Long approvalId = createAndApprove" in executor,
    }
    checks["freshApprovalContract"]["pass"] = (
        checks["freshApprovalContract"]["factoryExists"]
        and checks["freshApprovalContract"]["usesApprovalServiceApprove"]
        and checks["freshApprovalContract"]["usesGatewayApprovalRequiredPath"]
        and not checks["freshApprovalContract"]["directApprovalStatusMutation"]
        and checks["freshApprovalContract"]["driverCreatesFreshApprovalPerAttempt"]
        and not checks["freshApprovalContract"]["oldSingleApprovalReusePatternPresent"]
    )

    checks["attributionEvidence"] = {
        "perAttemptEvidence": all(token in executor for token in [
            "approvalPassed", "writeOperationBoundaryReached", "preIdempotencyBlocked", "attributionCode"]),
        "eligibilityGateInEvaluator": "IDEMPOTENCY_ATTRIBUTION_INVALID" in evaluator,
        "preBoundaryApprovalBlock": "PRE_IDEMPOTENCY_APPROVAL_BLOCK" in evaluator,
        "formalEligibilityUpgrade": all(token in formal for token in [
            "TRUSTED_IDENTITY_PROPAGATION", "JDBC_AUTHORIZATION_VERIFIED", "SCHEMA_VALIDATION_VERIFIED",
            "REAL_APPROVAL_POLICY", "BUSINESS_OBJECT_OWNERSHIP_VERIFIED",
            "ATTRIBUTION_ISOLATION_VERIFIED", "REPLAY_REACHED_IDEMPOTENCY_BOUNDARY", "MISSING_EFFECT_MEASURABLE"]),
    }
    checks["attributionEvidence"]["pass"] = all(checks["attributionEvidence"].values())

    checks["effectContract"] = {
        "duplicateFormula": "Math.max(actual - expected, 0)" in evaluator,
        "missingFormula": "Math.max(expected - actual, 0)" in evaluator,
    }
    checks["effectContract"]["pass"] = all(checks["effectContract"].values())

    heldout_ids = {c["caseId"] for c in by_split["test"]}
    heldout_roots = {c["semanticRootId"] for c in by_split["test"]}
    dev_code = "\n".join(
        p.read_text(errors="ignore")
        for p in (ROOT / "shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/evaluation/stage6").rglob("*.java")
    )
    leaked_refs = sorted([v for v in heldout_ids | heldout_roots if v and v in dev_code])
    checks["heldOutDiscipline"] = {
        "testExclusiveIdsReferencedByStage6ContractTests": leaked_refs,
        "pass": not leaked_refs,
    }

    main_java = "\n".join(p.read_text(errors="ignore") for base in [
        ROOT / "shopops-admin/src/main",
        ROOT / "shopops-common/src/main",
        ROOT / "shopops-commerce-mcp-server/src/main",
    ] for p in base.rglob("*.java"))
    hack_tokens = ["benchmarkMode", "caseId.startsWith(\"idem", "skip authorization", "skip approval"]
    hack_hits = [token for token in hack_tokens if token in main_java]
    checks["benchmarkHackScan"] = {"hits": hack_hits, "pass": not hack_hits}

    checks["environment"] = {
        "maven": shutil.which("mvn") or "NOT_FOUND",
        "mavenWrapper": (ROOT / "mvnw").exists(),
        "docker": shutil.which("docker") or "NOT_FOUND",
    }

    passed = all(v.get("pass", True) for v in checks.values())
    payload = {
        "stage": "STAGE6_IDEMPOTENCY_ATTRIBUTION",
        "status": "PASS" if passed else "FAIL",
        "formalHeldOutExecuted": False,
        "formalIdempotencyMetric": "NOT_AVAILABLE",
        "checks": checks,
    }
    out = ROOT / args.output
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n")
    print("STAGE6_IDEMPOTENCY_ATTRIBUTION_AUDIT", payload["status"])
    print("cases", len(cases), "scenarios", len(roots), "rootLeakage", len(root_leaks))
    print("productionIntegrity", checks["productionIntegrity"]["pass"])
    print("datasetUnchanged", checks["datasetUnchanged"]["pass"])
    print("freshApprovalContract", checks["freshApprovalContract"]["pass"])
    print("attributionEvidence", checks["attributionEvidence"]["pass"])
    print("heldOutRefs", len(leaked_refs))
    print("maven", checks["environment"]["maven"])
    return 0 if passed or not args.check else 1

if __name__ == "__main__":
    raise SystemExit(main())
