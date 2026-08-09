#!/usr/bin/env python3
"""Generate the frozen ShopOpsBench v1 dataset manifest from versioned benchmark resources."""
from __future__ import annotations
import hashlib, json
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BENCH = ROOT / "shopops-admin/src/test/resources/benchmark/v1"
OUT = BENCH / "benchmark-manifest-v1.json"
CONFIG = {
    "TASK": {"version": "1.1.0-phase2-task", "gold": "shopopsbench-gold-v1.1", "base": BENCH},
    "IDEMPOTENCY": {"version": "1.2.1-phase6-final-idempotency", "gold": "shopopsbench-gold-v1.2-idempotency", "base": BENCH / "idempotency"},
    "RECOVERY": {"version": "1.3.1-phase6-final-recovery", "gold": "shopopsbench-gold-v1.3-recovery", "base": BENCH / "recovery"},
    "GOVERNANCE": {"version": "1.4.1-phase6-final-governance", "gold": "shopopsbench-gold-v1.4-governance", "base": BENCH / "governance"},
}
SPLITS = ("dev", "validation", "test")

def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")

def sha(value):
    return hashlib.sha256(canonical(value)).hexdigest()

def load_selected(kind: str, base: Path, split: str):
    path = base / split / "cases.json"
    cases = json.loads(path.read_text(encoding="utf-8"))
    selected = [c for c in cases if c.get("benchmarkType") == kind]
    return path, selected

manifest = {
    "manifestVersion": "ShopOpsBench-v1-manifest-1",
    "benchmarkVersion": "ShopOpsBench-v1",
    "schemaVersion": "benchmark-case.schema.json",
    "schemaSha256": hashlib.sha256((BENCH / "benchmark-case.schema.json").read_bytes()).hexdigest(),
    "generatedAt": datetime.now(timezone.utc).isoformat(),
    "gitCommit": None,
    "gitMetadataAvailable": False,
    "benchmarks": {},
}
for kind, cfg in CONFIG.items():
    item = {"datasetVersion": cfg["version"], "goldVersion": cfg["gold"], "splits": {}, "caseCount": 0}
    for split in SPLITS:
        path, cases = load_selected(kind, cfg["base"], split)
        rel = path.relative_to(ROOT).as_posix()
        ids = [c["caseId"] for c in cases]
        item["splits"][split] = {
            "sourceFile": rel,
            "caseCount": len(cases),
            "caseIds": ids,
            "selectedCasesSha256": sha(cases),
        }
        item["caseCount"] += len(cases)
    item["heldOutTestCaseIds"] = item["splits"]["test"]["caseIds"]
    manifest["benchmarks"][kind] = item
manifest["formalHeldOutCaseCount"] = sum(v["splits"]["test"]["caseCount"] for v in manifest["benchmarks"].values())
OUT.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"WROTE {OUT}")
for kind, v in manifest["benchmarks"].items():
    print(kind, v["caseCount"], "held-out", v["splits"]["test"]["caseCount"])
print("FORMAL_HELD_OUT", manifest["formalHeldOutCaseCount"])
