from __future__ import annotations

import argparse
import json
import math
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def latest_json(directory: Path) -> Path | None:
    if not directory.exists():
        return None
    candidates = [p for p in directory.glob("*.json") if p.is_file()]
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime_ns)


def ratio(n: int | None, d: int | None) -> dict[str, Any]:
    if n is None or d is None or d <= 0:
        return {"numerator": n, "denominator": d, "rate": None, "percent": None, "wilson95": None, "status": "NOT_AVAILABLE"}
    r = n / d
    return {
        "numerator": n,
        "denominator": d,
        "rate": r,
        "percent": round(r * 100.0, 4),
        "wilson95": wilson(n, d),
        "status": "AVAILABLE",
    }


def wilson(success: int, total: int) -> dict[str, Any] | None:
    if total <= 0 or success < 0 or success > total:
        return None
    z = 1.959963984540054
    p = success / total
    z2 = z * z
    denom = 1 + z2 / total
    center = (p + z2 / (2 * total)) / denom
    margin = z * math.sqrt((p * (1 - p) + z2 / (4 * total)) / total) / denom
    return {
        "lower": max(0.0, center - margin),
        "upper": min(1.0, center + margin),
        "lowSampleSize": total < 20,
    }


def metric_obj(run: dict[str, Any] | None, key: str) -> dict[str, Any] | None:
    if not run:
        return None
    value = run.get(key)
    return value if isinstance(value, dict) else None


def task_summary(run: dict[str, Any] | None) -> dict[str, Any]:
    m = metric_obj(run, "taskMetrics")
    if not m:
        return {"status": "NOT_AVAILABLE", "taskSuccess": ratio(None, None)}
    return {
        "status": "AVAILABLE",
        "executed": m.get("executedCases"),
        "success": m.get("successCases"),
        "failed": m.get("failedCases"),
        "incorrectSuccess": m.get("incorrectSuccessCount"),
        "modelPlanAccepted": m.get("modelPlanAcceptedCount"),
        "modelFallback": m.get("modelFallbackCount"),
        "ruleBased": m.get("ruleBasedCount"),
        "taskSuccess": ratio(m.get("successCases"), m.get("executedCases")),
    }


def idem_summary(run: dict[str, Any] | None) -> dict[str, Any]:
    m = metric_obj(run, "idempotencyMetrics")
    if not m:
        return {"status": "NOT_AVAILABLE"}
    keys = [
        "executedCases", "logicalWriteRequests", "deliveryAttempts", "executionAttempts", "toolAttempts",
        "externalAttempts", "expectedEffectiveSideEffects", "actualEffectiveSideEffects", "duplicateSideEffects", "missingSideEffects",
    ]
    return {"status": "AVAILABLE", **{k: m.get(k) for k in keys}}


def recovery_summary(run: dict[str, Any] | None) -> dict[str, Any]:
    m = metric_obj(run, "recoveryMetrics")
    if not m:
        return {"status": "NOT_AVAILABLE", "convergence": ratio(None, None)}
    executed = m.get("executedCases")
    converged = m.get("converged")
    keys = ["faultCases", "executedCases", "terminalReached", "stateCorrect", "converged", "permanentStuck", "incorrectTerminalState", "manualReview", "duplicateSideEffects", "totalRecoveryAttempts"]
    return {"status": "AVAILABLE", **{k: m.get(k) for k in keys}, "convergence": ratio(converged, executed)}


def governance_summary(run: dict[str, Any] | None) -> dict[str, Any]:
    m = metric_obj(run, "governanceMetrics")
    if not m:
        return {"status": "NOT_AVAILABLE", "unauthorizedBlockRate": ratio(None, None), "falseRejectRate": ratio(None, None)}
    unauth = m.get("unauthorizedCasesExecuted")
    blocked = m.get("correctlyBlockedUnauthorizedCases")
    legitimate = m.get("legitimateCasesExecuted")
    false_reject = m.get("falseRejectedLegitimateCases")
    return {
        "status": "AVAILABLE",
        "unauthorizedCasesExecuted": unauth,
        "correctlyBlockedUnauthorizedCases": blocked,
        "legitimateCasesExecuted": legitimate,
        "falseRejectedLegitimateCases": false_reject,
        "unauthorizedWriteCount": m.get("unauthorizedWriteCount"),
        "approvalBypassCount": m.get("approvalBypassCount"),
        "crossTenantViolationCount": m.get("crossTenantViolationCount"),
        "crossShopViolationCount": m.get("crossShopViolationCount"),
        "unauthorizedBlockRate": ratio(blocked, unauth),
        "falseRejectRate": ratio(false_reject, legitimate),
    }


def release_gate(summary: dict[str, Any], gates: dict[str, Any]) -> dict[str, Any]:
    hard_cfg = gates.get("hardSafetyGates", gates.get("hardSafety", {}))
    quality_cfg = gates.get("qualityReliabilityGates", gates.get("quality", {}))
    idem = summary["benchmarks"]["idempotency"]
    gov = summary["benchmarks"]["governance"]
    task = summary["benchmarks"]["task"]
    rec = summary["benchmarks"]["recovery"]
    actual_hard = {
        "maxDuplicateSideEffects": idem.get("duplicateSideEffects"),
        "maxUnauthorizedWrites": gov.get("unauthorizedWriteCount"),
        "maxApprovalBypass": gov.get("approvalBypassCount"),
        "maxCrossTenantViolations": gov.get("crossTenantViolationCount"),
        "maxCrossShopViolations": gov.get("crossShopViolationCount"),
    }
    failures: list[str] = []
    unavailable: list[str] = []
    for name, max_allowed in hard_cfg.items():
        actual = actual_hard.get(name)
        if max_allowed is None or actual is None:
            unavailable.append(name)
        elif actual > max_allowed:
            failures.append(f"{name}: {actual} > {max_allowed}")
    q_actual = {
        "minimumTaskSuccess": task.get("taskSuccess", {}).get("rate"),
        "minimumStateConvergence": rec.get("convergence", {}).get("rate"),
        "maximumFalseRejectRate": gov.get("falseRejectRate", {}).get("rate"),
    }
    for name, threshold in quality_cfg.items():
        if name == "status":
            continue
        actual = q_actual.get(name)
        if threshold is None or actual is None:
            unavailable.append(name)
        elif name.startswith("minimum") and actual < threshold:
            failures.append(f"{name}: {actual} < {threshold}")
        elif name.startswith("maximum") and actual > threshold:
            failures.append(f"{name}: {actual} > {threshold}")
    status = "RELEASE_GATE_FAILED" if failures else ("RELEASE_GATE_PASS" if not unavailable else "RELEASE_GATE_NOT_AVAILABLE")
    return {"status": status, "failures": failures, "unavailable": unavailable, "configVersion": gates.get("gateVersion")}


def md(summary: dict[str, Any]) -> str:
    b = summary["benchmarks"]
    def rv(x: Any) -> str:
        return "NOT AVAILABLE" if x is None else str(x)
    lines = [
        "# ShopOpsBench v1 — Machine Aggregated Formal Summary", "",
        f"Generated: {summary['generatedAt']}", "",
        "This file is generated from formal benchmark JSON artifacts. Missing formal artifacts remain `NOT AVAILABLE`.", "",
        "## Task", "",
        f"- Task Success: {rv(b['task'].get('taskSuccess', {}).get('numerator'))} / {rv(b['task'].get('taskSuccess', {}).get('denominator'))}", "",
        "## Idempotency", "",
        f"- Logical writes: {rv(b['idempotency'].get('logicalWriteRequests'))}",
        f"- Repeated/delivery attempts: {rv(b['idempotency'].get('deliveryAttempts'))}",
        f"- External attempts: {rv(b['idempotency'].get('externalAttempts'))}",
        f"- Effective effects: {rv(b['idempotency'].get('actualEffectiveSideEffects'))}",
        f"- Duplicate effects: {rv(b['idempotency'].get('duplicateSideEffects'))}",
        f"- Missing effects: {rv(b['idempotency'].get('missingSideEffects'))}", "",
        "## Recovery", "",
        f"- Fault cases: {rv(b['recovery'].get('faultCases'))}",
        f"- Converged: {rv(b['recovery'].get('converged'))} / {rv(b['recovery'].get('executedCases'))}",
        f"- Permanent stuck: {rv(b['recovery'].get('permanentStuck'))}",
        f"- Manual review: {rv(b['recovery'].get('manualReview'))}", "",
        "## Governance", "",
        f"- Unauthorized blocked: {rv(b['governance'].get('correctlyBlockedUnauthorizedCases'))} / {rv(b['governance'].get('unauthorizedCasesExecuted'))}",
        f"- False reject: {rv(b['governance'].get('falseRejectedLegitimateCases'))} / {rv(b['governance'].get('legitimateCasesExecuted'))}",
        f"- Unauthorized writes: {rv(b['governance'].get('unauthorizedWriteCount'))}",
        f"- Approval bypass: {rv(b['governance'].get('approvalBypassCount'))}",
        f"- Cross tenant: {rv(b['governance'].get('crossTenantViolationCount'))}",
        f"- Cross shop: {rv(b['governance'].get('crossShopViolationCount'))}", "",
        "## Release Gate", "",
        f"- Status: **{summary['releaseGate']['status']}**", "",
    ]
    return "\n".join(lines)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--formal-root", type=Path, default=ROOT / "shopops-admin/target/benchmark/formal")
    ap.add_argument("--output", type=Path, default=ROOT / "artifacts/evaluation/formal-latest")
    args = ap.parse_args()
    manifest_path = ROOT / "shopops-admin/src/test/resources/benchmark/v1/benchmark-manifest-v1.json"
    gates_path = ROOT / "shopops-admin/src/test/resources/benchmark/v1/benchmark-gates-v1.json"
    manifest = read_json(manifest_path)
    gates = read_json(gates_path)
    runs: dict[str, Any] = {}
    sources: dict[str, Any] = {}
    for name in ["task", "idempotency", "recovery", "governance"]:
        p = latest_json(args.formal_root / name)
        sources[name] = str(p.relative_to(ROOT)) if p and p.is_relative_to(ROOT) else (str(p) if p else None)
        runs[name] = read_json(p) if p else None
    summary: dict[str, Any] = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "benchmarkVersion": manifest.get("benchmarkVersion"),
        "manifestVersion": manifest.get("manifestVersion"),
        "formalHeldOutCaseCount": manifest.get("formalHeldOutCaseCount"),
        "sources": sources,
        "benchmarks": {
            "task": task_summary(runs["task"]),
            "idempotency": idem_summary(runs["idempotency"]),
            "recovery": recovery_summary(runs["recovery"]),
            "governance": governance_summary(runs["governance"]),
        },
    }
    summary["releaseGate"] = release_gate(summary, gates)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "shopopsbench-final-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.output / "release-gate.json").write_text(json.dumps(summary["releaseGate"], ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.output / "FINAL_EVALUATION_REPORT.generated.md").write_text(md(summary), encoding="utf-8")
    print(f"WROTE {args.output}")
    print(f"RELEASE_GATE {summary['releaseGate']['status']}")
    for name, result in summary["benchmarks"].items():
        print(name.upper(), result.get("status"))


if __name__ == "__main__":
    main()
