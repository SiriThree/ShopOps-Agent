#!/usr/bin/env python3
import hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/"shopops-admin/src/test/resources/benchmark/v1/stage6-idempotency-evaluation-contract-manifest.json"

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def tree_hash(paths):
    rows=[]
    for base in paths:
        for p in sorted(base.rglob("*")):
            if p.is_file():
                rows.append(f"{p.relative_to(ROOT).as_posix()}\0{sha(p)}")
    return hashlib.sha256("\n".join(rows).encode()).hexdigest(),len(rows)

m=json.loads(MANIFEST.read_text())
errors=[]
if m.get("datasetVersion")!="1.3.0-stage5-idempotency-candidate": errors.append("dataset version changed")
if m.get("datasetCaseCount")!=22 or m.get("datasetSemanticScenarioCount")!=16: errors.append("dataset size changed")
if m.get("formalRunOccurred") is not False or m.get("heldOutExecutionOccurred") is not False: errors.append("formal execution incorrectly claimed")
for key in ("stage5CandidateManifest","stage5WorkloadProfile","attributionContract","workloadAttributionOverlay"):
    entry=m[key]
    path=ROOT/"shopops-admin/src/test/resources"/entry["path"] if entry["path"].startswith("benchmark/") else ROOT/entry["path"]
    if not path.exists(): errors.append(f"missing {key}: {path}")
    elif sha(path)!=entry["sha256"]: errors.append(f"hash mismatch {key}")
for key,entry in m.get("contractSourceHashes",{}).items():
    path=ROOT/entry["path"]
    if not path.exists(): errors.append(f"missing source {key}")
    elif sha(path)!=entry["sha256"]: errors.append(f"source hash mismatch {key}")
prod_hash,prod_count=tree_hash([
    ROOT/"shopops-admin/src/main",ROOT/"shopops-common/src/main",ROOT/"shopops-commerce-mcp-server/src/main"])
if prod_hash!=m["productionTree"]["sha256"] or prod_count!=m["productionTree"]["fileCount"]:
    errors.append("production tree mismatch")
if errors:
    print("STAGE6_IDEMPOTENCY_CONTRACT_MANIFEST_VERIFY FAIL")
    for e in errors: print("-",e)
    raise SystemExit(1)
print("STAGE6_IDEMPOTENCY_CONTRACT_MANIFEST_VERIFY PASS")
print("manifestSha256",sha(MANIFEST))
print("datasetVersion",m["datasetVersion"])
print("cases",m["datasetCaseCount"],"scenarios",m["datasetSemanticScenarioCount"])
print("formalMetric",m["formalIdempotencyMetric"])
