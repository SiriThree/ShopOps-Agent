#!/usr/bin/env python3
"""Verify frozen ShopOpsBench manifests or the Stage 2 Task expansion candidate manifest."""
from __future__ import annotations
import argparse, hashlib, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BENCH = ROOT / "shopops-admin/src/test/resources/benchmark/v1"
DEFAULT_MANIFEST = BENCH / "benchmark-manifest-v1.json"


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def sha(value):
    return hashlib.sha256(canonical(value)).hexdigest()


def resolve_manifest(value: str | None) -> Path:
    if not value:
        return DEFAULT_MANIFEST
    path = Path(value)
    if not path.is_absolute():
        direct = ROOT / path
        if direct.exists():
            return direct
        path = BENCH / path
    return path


def verify_frozen(m: dict) -> list[str]:
    errors=[]
    schema_hash=hashlib.sha256((BENCH/"benchmark-case.schema.json").read_bytes()).hexdigest()
    if schema_hash != m.get("schemaSha256"): errors.append("schemaSha256 mismatch")
    for kind,b in m["benchmarks"].items():
        for split,e in b["splits"].items():
            p=ROOT/e["sourceFile"]
            all_cases=json.loads(p.read_text(encoding="utf-8"))
            cases=[c for c in all_cases if c.get("benchmarkType")==kind]
            ids=[c["caseId"] for c in cases]
            if ids != e["caseIds"]: errors.append(f"{kind}/{split} caseIds changed")
            if len(cases)!=e["caseCount"]: errors.append(f"{kind}/{split} caseCount changed")
            if sha(cases)!=e["selectedCasesSha256"]: errors.append(f"{kind}/{split} dataset hash changed")
    return errors


def verify_task_candidate(m: dict) -> list[str]:
    errors=[]
    if m.get("status") != "EXPANSION_CANDIDATE": errors.append("candidate status must be EXPANSION_CANDIDATE")
    if m.get("formalRunOccurred") is not False: errors.append("formalRunOccurred must be false")
    schema_hash=hashlib.sha256((BENCH/"benchmark-case.schema.json").read_bytes()).hexdigest()
    if schema_hash != m.get("schemaSha256"): errors.append("schemaSha256 mismatch")
    all_ids=[]; all_roots=set()
    for split,e in m.get("splits",{}).items():
        p=ROOT/e["sourceFile"]
        all_cases=json.loads(p.read_text(encoding="utf-8"))
        cases=[c for c in all_cases if c.get("benchmarkType")=="TASK"]
        ids=[c["caseId"] for c in cases]
        roots={c.get("semanticRootId") for c in cases}
        if ids != e.get("caseIds"): errors.append(f"TASK/{split} caseIds changed")
        if len(cases)!=e.get("caseCount"): errors.append(f"TASK/{split} caseCount changed")
        if len(roots)!=e.get("semanticRootCount"): errors.append(f"TASK/{split} semanticRootCount changed")
        if sorted(roots)!=sorted(e.get("semanticRootIds",[])): errors.append(f"TASK/{split} semanticRootIds changed")
        if sha(cases)!=e.get("selectedCasesSha256"): errors.append(f"TASK/{split} dataset hash changed")
        all_ids.extend(ids); all_roots.update(roots)
    if len(all_ids)!=m.get("caseCount"): errors.append("candidate caseCount changed")
    if len(all_roots)!=m.get("semanticRootCount"): errors.append("candidate semanticRootCount changed")
    test_roots=set(m.get("splits",{}).get("test",{}).get("semanticRootIds",[]))
    if sorted(test_roots)!=sorted(m.get("heldOutRootIds",[])): errors.append("heldOutRootIds changed")
    root_map_path=ROOT/"shopops-admin/src/test/resources"/m.get("semanticRootMap","")
    if not root_map_path.exists():
        errors.append("semanticRootMap missing")
    elif hashlib.sha256(root_map_path.read_bytes()).hexdigest()!=m.get("rootMapSha256"):
        errors.append("rootMapSha256 mismatch")
    optional_hashes = {
        "goldProof": "goldProofSha256",
        "fixtureManifest": "fixtureManifestSha256",
        "rootFingerprint": "rootFingerprintSha256",
        "nearDuplicateReview": "nearDuplicateReviewSha256",
    }
    for path_key, hash_key in optional_hashes.items():
        rel=m.get(path_key)
        if not rel: continue
        path=ROOT/"shopops-admin/src/test/resources"/rel
        if not path.exists(): errors.append(f"{path_key} missing")
        elif hashlib.sha256(path.read_bytes()).hexdigest()!=m.get(hash_key): errors.append(f"{hash_key} mismatch")
    if m.get("caseHashes"):
        for split,e in m.get("splits",{}).items():
            pth=ROOT/e["sourceFile"]
            for c in json.loads(pth.read_text(encoding="utf-8")):
                if c.get("benchmarkType")=="TASK" and m["caseHashes"].get(c["caseId"]) != sha(c):
                    errors.append(f"case hash changed: {c['caseId']}")
    if m.get("trueTestExclusiveRootCount") is not None and len(test_roots)!=m.get("trueTestExclusiveRootCount"):
        errors.append("trueTestExclusiveRootCount changed")
    return errors



def verify_governance_candidate(m: dict) -> list[str]:
    errors=[]
    if m.get("status") != "EXPANSION_CANDIDATE": errors.append("candidate status must be EXPANSION_CANDIDATE")
    if m.get("formalRunOccurred") is not False: errors.append("formalRunOccurred must be false")
    if m.get("benchmarkType") != "GOVERNANCE": errors.append("benchmarkType must be GOVERNANCE")
    schema_hash=hashlib.sha256((BENCH/"benchmark-case.schema.json").read_bytes()).hexdigest()
    if schema_hash != m.get("schemaSha256"): errors.append("schemaSha256 mismatch")
    all_ids=[]; all_roots=set(); pos=set(); neg=set()
    for split,e in m.get("splits",{}).items():
        p=ROOT/e["sourceFile"]
        cases=json.loads(p.read_text(encoding="utf-8"))
        cases=[c for c in cases if c.get("benchmarkType")=="GOVERNANCE"]
        ids=[c["caseId"] for c in cases]
        roots={c.get("semanticRootId") for c in cases}
        if ids != e.get("caseIds"): errors.append(f"GOVERNANCE/{split} caseIds changed")
        if len(cases)!=e.get("caseCount"): errors.append(f"GOVERNANCE/{split} caseCount changed")
        if len(roots)!=e.get("semanticRootCount"): errors.append(f"GOVERNANCE/{split} semanticRootCount changed")
        if sorted(roots)!=sorted(e.get("semanticRootIds",[])): errors.append(f"GOVERNANCE/{split} semanticRootIds changed")
        if sha(cases)!=e.get("selectedCasesSha256"): errors.append(f"GOVERNANCE/{split} dataset hash changed")
        all_ids.extend(ids); all_roots.update(roots)
        for c in cases:
            (pos if c.get("governanceCaseClass")=="POSITIVE" else neg).add(c.get("semanticRootId"))
            if m.get("caseHashes",{}).get(c["caseId"]) != sha(c): errors.append(f"case hash changed: {c['caseId']}")
    if len(all_ids)!=m.get("caseCount"): errors.append("candidate caseCount changed")
    if len(all_roots)!=m.get("semanticRootCount"): errors.append("candidate semanticRootCount changed")
    if len(pos)!=m.get("positiveRootCount"): errors.append("positiveRootCount changed")
    if len(neg)!=m.get("negativeRootCount"): errors.append("negativeRootCount changed")
    test_cases=json.loads((ROOT/m["splits"]["test"]["sourceFile"]).read_text(encoding="utf-8"))
    test_pos={c.get("semanticRootId") for c in test_cases if c.get("governanceCaseClass")=="POSITIVE"}
    test_neg={c.get("semanticRootId") for c in test_cases if c.get("governanceCaseClass")=="NEGATIVE"}
    if len(test_pos)!=m.get("testPositiveRootCount"): errors.append("testPositiveRootCount changed")
    if len(test_neg)!=m.get("testNegativeRootCount"): errors.append("testNegativeRootCount changed")
    if sorted(test_pos)!=sorted(m.get("testPositiveRootIds",[])): errors.append("testPositiveRootIds changed")
    if sorted(test_neg)!=sorted(m.get("testNegativeRootIds",[])): errors.append("testNegativeRootIds changed")
    root_map_path=ROOT/"shopops-admin/src/test/resources"/m.get("semanticRootMap","")
    if not root_map_path.exists(): errors.append("semanticRootMap missing")
    elif hashlib.sha256(root_map_path.read_bytes()).hexdigest()!=m.get("rootMapSha256"): errors.append("rootMapSha256 mismatch")
    return errors


def verify_recovery_candidate(m: dict) -> list[str]:
    errors=[]
    if m.get("status") != "EXPANSION_CANDIDATE": errors.append("candidate status must be EXPANSION_CANDIDATE")
    if m.get("formalRunOccurred") is not False: errors.append("formalRunOccurred must be false")
    if m.get("benchmarkType") != "RECOVERY": errors.append("benchmarkType must be RECOVERY")
    schema_hash=hashlib.sha256((BENCH/"benchmark-case.schema.json").read_bytes()).hexdigest()
    if schema_hash != m.get("schemaSha256"): errors.append("schemaSha256 mismatch")
    all_ids=[]; all_roots=set()
    for split,e in m.get("splits",{}).items():
        p=ROOT/e["sourceFile"]
        cases=json.loads(p.read_text(encoding="utf-8"))
        cases=[c for c in cases if c.get("benchmarkType")=="RECOVERY"]
        ids=[c["caseId"] for c in cases]
        roots={c.get("semanticRootId") for c in cases}
        if ids != e.get("caseIds"): errors.append(f"RECOVERY/{split} caseIds changed")
        if len(cases)!=e.get("caseCount"): errors.append(f"RECOVERY/{split} caseCount changed")
        if len(roots)!=e.get("semanticRootCount"): errors.append(f"RECOVERY/{split} semanticRootCount changed")
        if sorted(roots)!=sorted(e.get("semanticRootIds",[])): errors.append(f"RECOVERY/{split} semanticRootIds changed")
        if sha(cases)!=e.get("selectedCasesSha256"): errors.append(f"RECOVERY/{split} dataset hash changed")
        all_ids.extend(ids); all_roots.update(roots)
        for c in cases:
            if m.get("caseHashes",{}).get(c["caseId"]) != sha(c): errors.append(f"case hash changed: {c['caseId']}")
    if len(all_ids)!=m.get("caseCount"): errors.append("candidate caseCount changed")
    if len(all_roots)!=m.get("semanticRootCount"): errors.append("candidate semanticRootCount changed")
    test_roots=set(m.get("splits",{}).get("test",{}).get("semanticRootIds",[]))
    if len(test_roots)!=m.get("testRootCount"): errors.append("testRootCount changed")
    if len(test_roots)!=m.get("trueTestExclusiveRootCount"): errors.append("trueTestExclusiveRootCount changed")
    if sorted(test_roots)!=sorted(m.get("heldOutRootIds",[])): errors.append("heldOutRootIds changed")
    root_map_path=ROOT/"shopops-admin/src/test/resources"/m.get("semanticRootMap","")
    if not root_map_path.exists(): errors.append("semanticRootMap missing")
    elif hashlib.sha256(root_map_path.read_bytes()).hexdigest()!=m.get("rootMapSha256"): errors.append("rootMapSha256 mismatch")
    return errors


def verify_idempotency_candidate(m: dict) -> list[str]:
    errors=[]
    if m.get("status") != "EXPANSION_CANDIDATE": errors.append("candidate status must be EXPANSION_CANDIDATE")
    if m.get("formalRunOccurred") is not False: errors.append("formalRunOccurred must be false")
    if m.get("benchmarkType") != "IDEMPOTENCY": errors.append("benchmarkType must be IDEMPOTENCY")
    schema_hash=hashlib.sha256((BENCH/"benchmark-case.schema.json").read_bytes()).hexdigest()
    if schema_hash != m.get("schemaSha256"): errors.append("schemaSha256 mismatch")
    all_ids=[]; all_roots=set()
    for split,e in m.get("splits",{}).items():
        p=ROOT/e["sourceFile"]
        cases=json.loads(p.read_text(encoding="utf-8"))
        cases=[c for c in cases if c.get("benchmarkType")=="IDEMPOTENCY"]
        ids=[c["caseId"] for c in cases]
        roots={c.get("semanticRootId") for c in cases}
        if ids != e.get("caseIds"): errors.append(f"IDEMPOTENCY/{split} caseIds changed")
        if len(cases)!=e.get("caseCount"): errors.append(f"IDEMPOTENCY/{split} caseCount changed")
        if len(roots)!=e.get("semanticRootCount"): errors.append(f"IDEMPOTENCY/{split} semanticRootCount changed")
        if sorted(roots)!=sorted(e.get("semanticRootIds",[])): errors.append(f"IDEMPOTENCY/{split} semanticRootIds changed")
        if sha(cases)!=e.get("selectedCasesSha256"): errors.append(f"IDEMPOTENCY/{split} dataset hash changed")
        all_ids.extend(ids); all_roots.update(roots)
        for c in cases:
            if m.get("caseHashes",{}).get(c["caseId"]) != sha(c): errors.append(f"case hash changed: {c['caseId']}")
    if len(all_ids)!=m.get("caseCount"): errors.append("candidate caseCount changed")
    if len(all_roots)!=m.get("semanticScenarioCount"): errors.append("candidate semanticScenarioCount changed")
    test_roots=set(m.get("splits",{}).get("test",{}).get("semanticRootIds",[]))
    if len(test_roots)!=m.get("testRootCount"): errors.append("testRootCount changed")
    if len(test_roots)!=m.get("trueTestExclusiveRootCount"): errors.append("trueTestExclusiveRootCount changed")
    if sorted(test_roots)!=sorted(m.get("heldOutRootIds",[])): errors.append("heldOutRootIds changed")
    root_map_path=ROOT/"shopops-admin/src/test/resources"/m.get("semanticRootMap","")
    if not root_map_path.exists(): errors.append("semanticRootMap missing")
    elif hashlib.sha256(root_map_path.read_bytes()).hexdigest()!=m.get("rootMapSha256"): errors.append("rootMapSha256 mismatch")
    workload_path=ROOT/"shopops-admin/src/test/resources"/m.get("workloadProfile","")
    if not workload_path.exists(): errors.append("workloadProfile missing")
    elif hashlib.sha256(workload_path.read_bytes()).hexdigest()!=m.get("workloadProfileSha256"): errors.append("workloadProfileSha256 mismatch")
    else:
        workload=json.loads(workload_path.read_text(encoding="utf-8"))
        formal=workload.get("profiles",{}).get("FORMAL",{})
        if formal.get("logicalOperationCount") != m.get("formalWorkloadLogicalOperationCount"): errors.append("formalWorkloadLogicalOperationCount changed")
        if formal.get("heldOutMetricLogicalOperations") != m.get("formalHeldOutLogicalOperationCount"): errors.append("formalHeldOutLogicalOperationCount changed")
        if formal.get("plannedRepeatedRequestAttempts") != m.get("formalWorkloadPlannedAttempts"): errors.append("formalWorkloadPlannedAttempts changed")
    return errors

def main() -> int:
    parser=argparse.ArgumentParser()
    parser.add_argument("--manifest", help="manifest path relative to repo root or benchmark/v1")
    args=parser.parse_args()
    manifest=resolve_manifest(args.manifest)
    if not manifest.exists():
        print("BENCHMARK_MANIFEST_VERIFY FAIL")
        print(" - manifest missing:",manifest)
        return 1
    m=json.loads(manifest.read_text(encoding="utf-8"))
    if m.get("status") == "EXPANSION_CANDIDATE" and m.get("benchmarkType") == "IDEMPOTENCY":
        errors=verify_idempotency_candidate(m)
        label="IDEMPOTENCY_CANDIDATE_MANIFEST_VERIFY"
    elif m.get("status") == "EXPANSION_CANDIDATE" and m.get("benchmarkType") == "GOVERNANCE":
        errors=verify_governance_candidate(m)
        label="GOVERNANCE_CANDIDATE_MANIFEST_VERIFY"
    elif m.get("status") == "EXPANSION_CANDIDATE" and m.get("benchmarkType") == "RECOVERY":
        errors=verify_recovery_candidate(m)
        label="RECOVERY_CANDIDATE_MANIFEST_VERIFY"
    elif m.get("status") == "EXPANSION_CANDIDATE" or "splits" in m and "benchmarks" not in m:
        errors=verify_task_candidate(m)
        label="TASK_CANDIDATE_MANIFEST_VERIFY"
    else:
        errors=verify_frozen(m)
        label="BENCHMARK_MANIFEST_VERIFY"
    if errors:
        print(label,"FAIL")
        for x in errors: print(" -",x)
        return 1
    print(label,"PASS")
    print("manifest",manifest.relative_to(ROOT))
    if "formalHeldOutCaseCount" in m: print("formalHeldOutCaseCount",m["formalHeldOutCaseCount"])
    if "caseCount" in m: print("caseCount",m["caseCount"])
    if "semanticRootCount" in m: print("semanticRootCount",m["semanticRootCount"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
