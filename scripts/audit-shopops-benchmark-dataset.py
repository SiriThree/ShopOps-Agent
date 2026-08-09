#!/usr/bin/env python3
"""Deterministic ShopOpsBench dataset quality audit.

The audit never executes the Agent or calls an LLM. It validates current benchmark
resources against the latest semantic-root audit overlay and emits machine-readable
coverage, leakage, provenance, and near-duplicate evidence. Dataset mutation is
performed by explicit expansion stages, not by this audit script.
"""
from __future__ import annotations

import argparse
import hashlib
import itertools
import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

try:
    import jsonschema
except Exception as exc:  # pragma: no cover - environment guard
    jsonschema = None
    JSONSCHEMA_IMPORT_ERROR = str(exc)
else:
    JSONSCHEMA_IMPORT_ERROR = None

BENCHMARKS = ("TASK", "IDEMPOTENCY", "RECOVERY", "GOVERNANCE")
SPLITS = ("dev", "validation", "test")


def repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def resource_root(repo: Path) -> Path:
    return repo / "shopops-admin/src/test/resources/benchmark/v1"


def split_of(path: Path, root: Path) -> str:
    parts = path.relative_to(root).parts
    if "smoke" in parts:
        return "smoke"
    for split in SPLITS:
        if split in parts:
            return split
    return "other"


def resource_role(path: Path, case: dict[str, Any], root: Path) -> str:
    rel = path.relative_to(root).as_posix()
    benchmark_type = case.get("benchmarkType")
    if benchmark_type == "TASK" and rel in {"dev/cases.json", "validation/cases.json", "test/cases.json"}:
        return "DEDICATED"
    if benchmark_type == "IDEMPOTENCY" and rel.startswith("idempotency/"):
        return "DEDICATED"
    if benchmark_type == "RECOVERY" and rel.startswith("recovery/"):
        return "DEDICATED"
    if benchmark_type == "GOVERNANCE" and rel.startswith("governance/"):
        return "DEDICATED"
    if rel.startswith("smoke/"):
        return "SMOKE"
    return "CONTRACT_EXAMPLE"


def load_cases(root: Path) -> list[dict[str, Any]]:
    loaded: list[dict[str, Any]] = []
    for path in sorted(root.glob("**/*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        if not isinstance(data, list) or not all(isinstance(item, dict) and "caseId" in item for item in data):
            continue
        for case in data:
            loaded.append({
                "path": path,
                "case": case,
                "split": split_of(path, root),
                "resourceRole": resource_role(path, case, root),
            })
    return loaded


def load_mapping(root: Path) -> tuple[dict[str, Any], dict[str, dict[str, Any]]]:
    stage7b = root / "audit/stage7b-semantic-root-map.json"
    stage7a = root / "audit/stage7a-semantic-root-map.json"
    stage5 = root / "audit/stage5-semantic-root-map.json"
    stage4 = root / "audit/stage4-semantic-root-map.json"
    stage3 = root / "audit/stage3-semantic-root-map.json"
    stage2 = root / "audit/stage2-semantic-root-map.json"
    path = stage7b if stage7b.exists() else (stage7a if stage7a.exists() else (stage5 if stage5.exists() else (stage4 if stage4.exists() else (stage3 if stage3.exists() else (stage2 if stage2.exists() else root / "audit/stage1-semantic-root-map.json")))))
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = {entry["caseId"]: entry for entry in data.get("cases", [])}
    return data, entries


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "").lower()
    value = re.sub(r"[^\w\u4e00-\u9fff]+", " ", value, flags=re.UNICODE)
    return re.sub(r"\s+", " ", value).strip()


def normalize_obj(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: normalize_obj(value[key]) for key in sorted(value)}
    if isinstance(value, list):
        return [normalize_obj(item) for item in value]
    if isinstance(value, str):
        return normalize_text(value)
    return value


def runtime_payload(case: dict[str, Any]) -> dict[str, Any]:
    benchmark_type = case.get("benchmarkType")
    if benchmark_type == "TASK":
        return {"input": case.get("input"), "identity": case.get("identity")}
    if benchmark_type in {"IDEMPOTENCY", "RECOVERY"}:
        return {
            "input": case.get("input"),
            "identity": case.get("identity"),
            "faultInjection": case.get("faultInjection"),
        }
    if benchmark_type == "GOVERNANCE":
        return {
            "identity": case.get("identity"),
            "toolCode": case.get("toolCode") or (case.get("input") or {}).get("toolCode"),
            "arguments": case.get("arguments") or case.get("input"),
            "initialState": case.get("initialState"),
        }
    return {"input": case.get("input")}


def canonical(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def char_ngrams(text: str, n: int = 3) -> set[str]:
    compact = normalize_text(text).replace(" ", "")
    if not compact:
        return set()
    if len(compact) < n:
        return {compact}
    return {compact[i:i+n] for i in range(len(compact) - n + 1)}


def jaccard(left: set[str], right: set[str]) -> float:
    if not left and not right:
        return 1.0
    union = left | right
    return len(left & right) / len(union) if union else 0.0


def user_text(case: dict[str, Any]) -> str:
    return str((case.get("input") or {}).get("userInput") or "")


def lexical_similarity(left: dict[str, Any], right: dict[str, Any]) -> float:
    ltxt, rtxt = user_text(left), user_text(right)
    if ltxt or rtxt:
        return jaccard(char_ngrams(ltxt), char_ngrams(rtxt))
    # Structured scenarios use semantic/root lineage rather than pretending IDs are natural-language similarity.
    return 0.0


def governance_similarity(case: dict[str, Any]) -> str:
    args = dict(case.get("arguments") or {})
    for key in ("operationRequestId", "approvalId"):
        if key in args:
            args[key] = f"<{key}>"
    identity = dict(case.get("identity") or {})
    identity.pop("userId", None)
    return canonical({
        "toolCode": case.get("toolCode"),
        "identity": identity,
        "arguments": args,
        "approvalSetup": (case.get("initialState") or {}).get("approvalSetup"),
    })

def governance_pair_similarity(left: dict[str, Any], right: dict[str, Any]) -> float:
    return jaccard(char_ngrams(governance_similarity(left)), char_ngrams(governance_similarity(right)))


def recovery_similarity(case: dict[str, Any]) -> str:
    fault = dict(case.get("faultInjection") or {})
    # triggerAt/seed/request IDs are execution metadata, not causal identity.
    fault.pop("triggerAt", None)
    return canonical({
        "scenario": case.get("scenario"),
        "faultType": case.get("faultType"),
        "faultPoint": case.get("faultPoint"),
        "faultInjection": fault,
        "initialLocalState": case.get("initialLocalState"),
        "initialExternalState": case.get("initialExternalState"),
        "expectedTerminalStates": case.get("expectedTerminalStates"),
        "expectedExternalState": case.get("expectedExternalState"),
        "expectedConvergence": case.get("expectedConvergence"),
        "manualReviewAllowed": case.get("manualReviewAllowed"),
        "maxRecoveryAttempts": case.get("maxRecoveryAttempts"),
        "concurrent": bool((case.get("concurrency") or {}).get("simultaneous")),
    })


def recovery_pair_similarity(left: dict[str, Any], right: dict[str, Any]) -> float:
    return jaccard(char_ngrams(recovery_similarity(left)), char_ngrams(recovery_similarity(right)))




def idempotency_similarity(case: dict[str, Any]) -> str:
    expectation = dict(case.get("idempotencyExpectation") or {})
    return canonical({
        "keyRelation": expectation.get("keyRelation"),
        "payloadRelation": expectation.get("payloadRelation"),
        "repeatPattern": expectation.get("repeatPattern"),
        "faultSemantics": expectation.get("faultSemantics"),
        "expectedLogicalEffects": case.get("expectedEffectiveSideEffects"),
        "externalMode": case.get("externalSystemMode"),
        "concurrent": bool((case.get("concurrency") or {}).get("simultaneous")),
    })


def idempotency_pair_similarity(left: dict[str, Any], right: dict[str, Any]) -> float:
    return jaccard(char_ngrams(idempotency_similarity(left)), char_ngrams(idempotency_similarity(right)))

def schema_errors(cases: list[dict[str, Any]], root: Path) -> list[dict[str, str]]:
    if jsonschema is None:
        return [{"caseId": "<environment>", "error": f"jsonschema unavailable: {JSONSCHEMA_IMPORT_ERROR}"}]
    schema = json.loads((root / "benchmark-case.schema.json").read_text(encoding="utf-8"))
    validator = jsonschema.Draft202012Validator(schema)
    errors: list[dict[str, str]] = []
    for item in cases:
        for error in validator.iter_errors(item["case"]):
            errors.append({"caseId": item["case"]["caseId"], "error": error.message})
    return errors


def exact_duplicate_pairs(cases: list[dict[str, Any]], normalized: bool = False) -> list[dict[str, Any]]:
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in cases:
        payload = runtime_payload(item["case"])
        if normalized:
            payload = normalize_obj(payload)
        groups[canonical(payload)].append(item)
    pairs: list[dict[str, Any]] = []
    for group in groups.values():
        if len(group) < 2:
            continue
        for left, right in itertools.combinations(group, 2):
            pairs.append({
                "leftCaseId": left["case"]["caseId"],
                "rightCaseId": right["case"]["caseId"],
                "leftSplit": left["split"],
                "rightSplit": right["split"],
            })
    return pairs




def audit_signature(case: dict[str, Any]) -> dict[str, Any]:
    return {
        "runtimePayload": runtime_payload(case),
        "expectedOutcome": case.get("expectedOutcome"),
        "requiredCapabilities": case.get("requiredCapabilities"),
        "optionalCapabilities": case.get("optionalCapabilities"),
        "acceptableTools": case.get("acceptableTools"),
        "forbiddenTools": case.get("forbiddenTools"),
        "sideEffectExpectation": case.get("sideEffectExpectation"),
        "approvalExpectation": case.get("approvalExpectation"),
        "expectedDecision": case.get("expectedDecision"),
        "expectedReason": case.get("expectedReason"),
        "expectedExternalState": case.get("expectedExternalState"),
        "expectedConvergence": case.get("expectedConvergence"),
    }


def duplicate_pairs_by_signature(cases: list[dict[str, Any]], normalized: bool = False) -> list[dict[str, Any]]:
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in cases:
        signature = audit_signature(item["case"])
        if normalized:
            signature = normalize_obj(signature)
        groups[canonical(signature)].append(item)
    pairs: list[dict[str, Any]] = []
    for group in groups.values():
        if len(group) < 2:
            continue
        for left, right in itertools.combinations(group, 2):
            pairs.append({
                "leftCaseId": left["case"]["caseId"],
                "rightCaseId": right["case"]["caseId"],
                "leftSplit": left["split"],
                "rightSplit": right["split"],
            })
    return pairs

def recursive_has_key(value: Any, key: str) -> bool:
    if isinstance(value, dict):
        return key in value or any(recursive_has_key(v, key) for v in value.values())
    if isinstance(value, list):
        return any(recursive_has_key(v, key) for v in value)
    return False


def root_groups(cases: list[dict[str, Any]], mapping: dict[str, dict[str, Any]]) -> dict[str, list[dict[str, Any]]]:
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in cases:
        entry = mapping[item["case"]["caseId"]]
        groups[entry["semanticRootId"]].append(item)
    return groups


def cross_split_root_leaks(cases: list[dict[str, Any]], mapping: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    leaks = []
    for root_id, group in root_groups(cases, mapping).items():
        split_names = sorted({item["split"] for item in group if item["split"] in SPLITS})
        if len(split_names) <= 1:
            continue
        leaks.append({
            "semanticRootId": root_id,
            "splits": split_names,
            "cases": [{"caseId": item["case"]["caseId"], "split": item["split"]} for item in group],
        })
    return sorted(leaks, key=lambda x: x["semanticRootId"])


def cross_split_parent_leaks(cases: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_id = {item["case"]["caseId"]: item for item in cases}
    issues = []
    for item in cases:
        parent_id = item["case"].get("parentCaseId")
        parent = by_id.get(parent_id)
        if parent and item["split"] != parent["split"]:
            issues.append({
                "caseId": item["case"]["caseId"], "split": item["split"],
                "parentCaseId": parent_id, "parentSplit": parent["split"],
            })
    return issues


def load_task_root_fingerprints(root: Path) -> dict[str, dict[str, Any]]:
    path = root / "task/stage7a/task-root-fingerprints.json"
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return {item["semanticRootId"]: item for item in data.get("roots", [])}


def load_governance_root_fingerprints(root: Path) -> dict[str, dict[str, Any]]:
    path = root / "governance/scaleup/governance-root-fingerprints.json"
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return {item["semanticRootId"]: item for item in data.get("roots", [])}


def structural_fingerprint(item: dict[str, Any]) -> str:
    fields = (
        "businessScenario", "businessGoalClass", "fixtureStateClass",
        "expectedOutcomeClass", "requiredEvidenceSignature", "resultClass",
        "dateSemantics", "difficultyDimensions", "fixtureStateSignature",
    )
    return json.dumps({key: item.get(key) for key in fields}, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def near_duplicate_candidates(cases: list[dict[str, Any]], mapping: dict[str, dict[str, Any]], root: Path | None = None) -> list[dict[str, Any]]:
    candidates: list[dict[str, Any]] = []
    task_fingerprints = load_task_root_fingerprints(root) if root is not None else {}
    governance_fingerprints = load_governance_root_fingerprints(root) if root is not None else {}
    for left, right in itertools.combinations(cases, 2):
        lcase, rcase = left["case"], right["case"]
        if lcase.get("benchmarkType") != rcase.get("benchmarkType"):
            continue
        lroot = mapping[lcase["caseId"]]["semanticRootId"]
        rroot = mapping[rcase["caseId"]]["semanticRootId"]
        benchmark_type = lcase.get("benchmarkType")
        similarity = lexical_similarity(lcase, rcase)
        if benchmark_type == "GOVERNANCE":
            similarity = governance_pair_similarity(lcase, rcase)
        if benchmark_type == "RECOVERY":
            similarity = recovery_pair_similarity(lcase, rcase)
        if benchmark_type == "IDEMPOTENCY":
            similarity = idempotency_pair_similarity(lcase, rcase)
        same_root = lroot == rroot
        same_parent = lcase.get("parentCaseId") == rcase.get("caseId") or rcase.get("parentCaseId") == lcase.get("caseId")
        high_text_similarity = benchmark_type == "TASK" and similarity >= 0.72
        high_governance_similarity = benchmark_type == "GOVERNANCE" and similarity >= 0.72
        high_recovery_similarity = benchmark_type == "RECOVERY" and similarity >= 0.72
        high_idempotency_similarity = benchmark_type == "IDEMPOTENCY" and similarity >= 0.72
        paired = mapping[lcase["caseId"]].get("pairedRootId") == rroot or mapping[rcase["caseId"]].get("pairedRootId") == lroot
        if benchmark_type == "TASK":
            lfp, rfp = task_fingerprints.get(lroot), task_fingerprints.get(rroot)
        elif benchmark_type == "GOVERNANCE":
            lfp, rfp = governance_fingerprints.get(lroot), governance_fingerprints.get(rroot)
        else:
            lfp = rfp = None
        same_root_fingerprint = bool(lfp and rfp and lroot != rroot and lfp.get("fingerprintHash") == rfp.get("fingerprintHash"))
        same_fixture_state_signature = bool(lfp and rfp and lfp.get("fixtureStateSignature") == rfp.get("fixtureStateSignature"))
        same_gold_signature = normalize_obj(audit_signature(lcase)) == normalize_obj(audit_signature(rcase))
        if not (same_root or same_parent or high_text_similarity or high_governance_similarity or high_recovery_similarity or high_idempotency_similarity or paired or same_root_fingerprint):
            continue
        decision = "SAME_ROOT" if same_root else "REVIEW_REQUIRED"
        candidates.append({
            "leftCaseId": lcase["caseId"],
            "rightCaseId": rcase["caseId"],
            "benchmarkType": lcase.get("benchmarkType"),
            "leftSplit": left["split"],
            "rightSplit": right["split"],
            "similarity": round(similarity, 6),
            "sameSemanticRoot": same_root,
            "semanticRootId": lroot if same_root else None,
            "decision": decision,
            "pairedControl": paired,
            "sameRootFingerprint": same_root_fingerprint,
            "sameFixtureStateSignature": same_fixture_state_signature,
            "sameGoldSignature": same_gold_signature,
            "leftRootFingerprint": lfp.get("fingerprintHash") if lfp else None,
            "rightRootFingerprint": rfp.get("fingerprintHash") if rfp else None,
            "crossSplit": left["split"] != right["split"],
        })
    return sorted(candidates, key=lambda x: (x["benchmarkType"], not x["crossSplit"], x["leftCaseId"], x["rightCaseId"]))


def search_case_references(repo: Path, cases: list[dict[str, Any]]) -> dict[str, list[dict[str, str]]]:
    production_files = list((repo / "shopops-admin/src/main/java").rglob("*.java"))
    evaluator_files = list((repo / "shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/evaluator").rglob("*.java"))
    production_hits = []
    evaluator_input_hits = []
    production_text = [(path, path.read_text(encoding="utf-8", errors="ignore")) for path in production_files]
    evaluator_text = [(path, path.read_text(encoding="utf-8", errors="ignore")) for path in evaluator_files]
    for item in cases:
        case_id = item["case"]["caseId"]
        for path, text in production_text:
            if case_id in text:
                production_hits.append({"caseId": case_id, "file": path.relative_to(repo).as_posix()})
        if item["split"] == "test":
            text_value = user_text(item["case"]).strip()
            if len(text_value) >= 12:
                for path, text in evaluator_text:
                    if text_value in text:
                        evaluator_input_hits.append({"caseId": case_id, "file": path.relative_to(repo).as_posix()})
    return {"productionCaseIdReferences": production_hits, "heldOutInputEvaluatorReferences": evaluator_input_hits}


def existing_semantic_task_id_issues(cases: list[dict[str, Any]], mapping: dict[str, dict[str, Any]]) -> dict[str, Any]:
    sem_to_roots: dict[str, set[str]] = defaultdict(set)
    root_to_sem: dict[str, set[str]] = defaultdict(set)
    for item in cases:
        case = item["case"]
        sem = case.get("semanticTaskId")
        if not sem:
            continue
        root = mapping[case["caseId"]]["semanticRootId"]
        sem_to_roots[sem].add(root)
        root_to_sem[root].add(sem)
    false_merges = {sem: sorted(roots) for sem, roots in sem_to_roots.items() if len(roots) > 1}
    fragmented_roots = {root: sorted(values) for root, values in root_to_sem.items() if len(values) > 1}
    return {"semanticTaskIdFalseMergeCount": len(false_merges), "falseMerges": false_merges,
            "semanticRootFragmentationCount": len(fragmented_roots), "fragmentedRoots": fragmented_roots}


def benchmark_summary(dedicated: list[dict[str, Any]], mapping: dict[str, dict[str, Any]]) -> dict[str, Any]:
    summary = {}
    for benchmark in BENCHMARKS:
        subset = [item for item in dedicated if item["case"].get("benchmarkType") == benchmark]
        roots = root_groups(subset, mapping)
        test_roots = {root for root, group in roots.items() if any(item["split"] == "test" for item in group)}
        exclusive_test_roots = {
            root for root in test_roots
            if {item["split"] for item in roots[root] if item["split"] in SPLITS} == {"test"}
        }
        summary[benchmark] = {
            "caseCount": len(subset),
            "semanticRootCount": len(roots),
            "splitCaseCounts": dict(Counter(item["split"] for item in subset)),
            "heldOutCaseCount": sum(item["split"] == "test" for item in subset),
            "heldOutSemanticRootCount": len(test_roots),
            "testExclusiveSemanticRootCount": len(exclusive_test_roots),
            "crossSplitRootLeakCount": len(cross_split_root_leaks(subset, mapping)),
        }
    return summary


def coverage(dedicated: list[dict[str, Any]], mapping: dict[str, dict[str, Any]]) -> dict[str, Any]:
    task = [item for item in dedicated if item["case"].get("benchmarkType") == "TASK"]
    governance = [item for item in dedicated if item["case"].get("benchmarkType") == "GOVERNANCE"]
    recovery = [item for item in dedicated if item["case"].get("benchmarkType") == "RECOVERY"]
    idempotency = [item for item in dedicated if item["case"].get("benchmarkType") == "IDEMPOTENCY"]

    task_root_by_scenario: dict[str, set[str]] = defaultdict(set)
    for item in task:
        task_root_by_scenario[item["case"].get("scenario")].add(mapping[item["case"]["caseId"]]["semanticRootId"])
    task_dates = Counter()
    task_shops = Counter()
    for item in task:
        case = item["case"]
        date_range = (case.get("input") or {}).get("dateRange")
        task_dates[canonical(date_range)] += 1
        task_shops[str((case.get("identity") or {}).get("shopId"))] += 1

    gov_root_by_attack: dict[str, set[str]] = defaultdict(set)
    for item in governance:
        gov_root_by_attack[item["case"].get("attackType")].add(mapping[item["case"]["caseId"]]["semanticRootId"])
    gov_positive = [item for item in governance if item["case"].get("governanceCaseClass") == "POSITIVE"]
    gov_negative = [item for item in governance if item["case"].get("governanceCaseClass") == "NEGATIVE"]
    gov_pos_roots = {mapping[item["case"]["caseId"]]["semanticRootId"] for item in gov_positive}
    gov_neg_roots = {mapping[item["case"]["caseId"]]["semanticRootId"] for item in gov_negative}

    recovery_roots = root_groups(recovery, mapping)
    idempotency_roots = root_groups(idempotency, mapping)

    return {
        "task": {
            "scenarioCases": dict(Counter(item["case"].get("scenario") for item in task)),
            "scenarioSemanticRoots": {key: len(value) for key, value in sorted(task_root_by_scenario.items())},
            "difficulty": dict(Counter(item["case"].get("difficulty") for item in task)),
            "tags": dict(Counter(tag for item in task for tag in item["case"].get("tags", []))),
            "fixtureDateRanges": dict(task_dates),
            "shopConcentration": dict(task_shops),
            "explicitNaturalLanguageVariantTags": sum("NATURAL_LANGUAGE_VARIANT" in item["case"].get("tags", []) for item in task),
            "rootDerivedAdditionalVariants": len(task) - len(root_groups(task, mapping)),
        },
        "governance": {
            "negativeCases": len(gov_negative),
            "positiveCases": len(gov_positive),
            "negativeSemanticRoots": len(gov_neg_roots),
            "positiveSemanticRoots": len(gov_pos_roots),
            "heldOutPositiveCases": sum(item["split"] == "test" for item in gov_positive),
            "heldOutNegativeCases": sum(item["split"] == "test" for item in gov_negative),
            "testExclusivePositiveRoots": len({r for r in gov_pos_roots if {x["split"] for x in root_groups(governance, mapping)[r]} == {"test"}}),
            "testExclusiveNegativeRoots": len({r for r in gov_neg_roots if {x["split"] for x in root_groups(governance, mapping)[r]} == {"test"}}),
            "pairedSemanticRoots": len({mapping[item["case"]["caseId"]].get("semanticRootId") for item in governance if mapping[item["case"]["caseId"]].get("pairedRootId")}),
            "attackTypeCases": dict(Counter(item["case"].get("attackType") for item in governance)),
            "attackTypeSemanticRoots": {key: len(value) for key, value in sorted(gov_root_by_attack.items(), key=lambda kv: str(kv[0]))},
        },
        "recovery": {
            "semanticScenarios": len(recovery_roots),
            "faultTypeCases": dict(Counter(item["case"].get("faultType") for item in recovery)),
            "faultPointCases": dict(Counter(str(item["case"].get("faultPoint")) for item in recovery)),
            "initialLocalStates": dict(Counter(item["case"].get("initialLocalState") for item in recovery)),
            "initialExternalStates": dict(Counter(item["case"].get("initialExternalState") for item in recovery)),
            "recoveryBudgets": dict(Counter(str(item["case"].get("maxRecoveryAttempts")) for item in recovery)),
            "concurrentCases": sum(bool((item["case"].get("concurrency") or {}).get("simultaneous")) for item in recovery),
            "manualReviewAllowedCases": sum(item["case"].get("manualReviewAllowed") is True for item in recovery),
        },
        "idempotency": {
            "semanticScenarios": len(idempotency_roots),
            "configuredLogicalOperations": sum(int(item["case"].get("logicalWriteCount") or 0) for item in idempotency),
            "configuredDeliveryAttempts": sum(int((item["case"].get("deliveryPattern") or {}).get("attempts") or 0) for item in idempotency),
            "deliveryModes": dict(Counter((item["case"].get("deliveryPattern") or {}).get("mode") for item in idempotency)),
            "concurrencyWorkers": dict(Counter(str((item["case"].get("concurrency") or {}).get("workers")) for item in idempotency)),
            "scenarioCases": dict(Counter(item["case"].get("scenario") for item in idempotency)),
        },
    }


def apply_near_duplicate_review(root: Path, candidates: list[dict[str, Any]], benchmark: str | None) -> tuple[list[dict[str, Any]], dict[str, int]]:
    if benchmark == "governance":
        stage7b_review = root / "governance/scaleup/governance-near-duplicate-review.json"
        review_path = stage7b_review if stage7b_review.exists() else root / "governance/stage3/governance-near-duplicate-review.json"
    elif benchmark == "recovery":
        review_path = root / "recovery/stage4/recovery-near-duplicate-review.json"
    elif benchmark == "idempotency":
        review_path = root / "idempotency/stage5/idempotency-near-duplicate-review.json"
    else:
        stage7a_review = root / "task/stage7a/task-near-duplicate-review.json"
        review_path = stage7a_review if stage7a_review.exists() else root / "task/stage2/task-near-duplicate-review.json"
    if not review_path.exists():
        return candidates, {"reviewed": 0, "keepDistinct": 0, "sameRoot": 0, "rejectedOrMerged": 0, "unresolved": sum(item.get("decision") == "REVIEW_REQUIRED" for item in candidates)}
    doc = json.loads(review_path.read_text(encoding="utf-8"))
    lookup = {}
    for item in doc.get("decisions", []):
        key = tuple(sorted((item.get("leftCaseId"), item.get("rightCaseId"))))
        lookup[key] = item
    reviewed=[]
    counts=Counter()
    for item in candidates:
        key=tuple(sorted((item.get("leftCaseId"),item.get("rightCaseId"))))
        review=lookup.get(key)
        merged=dict(item)
        if review:
            merged["reviewDecision"]=review.get("reviewDecision")
            merged["reviewStatus"]=review.get("reviewStatus")
            merged["reviewRationale"]=review.get("rationale")
            counts["reviewed"]+=1
            decision=review.get("reviewDecision")
            if decision in {"KEEP_DISTINCT", "KEEP_DISTINCT_PAIRED_CONTROL"}: counts["keepDistinct"]+=1
            elif decision=="SAME_ROOT": counts["sameRoot"]+=1
            elif decision in {"MERGED","REJECTED","LIKELY_DUPLICATE"}: counts["rejectedOrMerged"]+=1
        if item.get("decision")=="REVIEW_REQUIRED" and not review:
            counts["unresolved"]+=1
        reviewed.append(merged)
    return reviewed, dict(counts)

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="artifacts/evaluation/dataset-audit/stage1-audit.json")
    parser.add_argument("--check", action="store_true", help="Exit non-zero only for structural audit-contract failures, not discovered dataset quality risks.")
    parser.add_argument("--benchmark", choices=["task", "idempotency", "recovery", "governance"], help="Restrict audit/reporting to one benchmark while reusing the same deterministic audit engine.")
    args = parser.parse_args()

    repo = repo_root()
    root = resource_root(repo)
    cases = load_cases(root)
    mapping_doc, mapping = load_mapping(root)
    if args.benchmark:
        selected_type = args.benchmark.upper()
        cases = [item for item in cases if item["case"].get("benchmarkType") == selected_type]
    all_case_ids = {item["case"]["caseId"] for item in cases}
    mapped_ids = set(mapping)
    mapping_missing = sorted(all_case_ids - mapped_ids)
    if args.benchmark:
        mapped_ids_in_scope = {case_id for case_id, entry in mapping.items() if entry.get("benchmarkType") == args.benchmark.upper()}
        mapping_unknown = sorted(mapped_ids_in_scope - all_case_ids)
    else:
        mapping_unknown = sorted(mapped_ids - all_case_ids)
    dedicated = [item for item in cases if item["resourceRole"] == "DEDICATED"]

    schema = schema_errors(cases, root)
    dedicated_leaks = cross_split_root_leaks(dedicated, mapping) if not mapping_missing else []
    all_leaks = cross_split_root_leaks(cases, mapping) if not mapping_missing else []
    parent_leaks = cross_split_parent_leaks(cases)
    exact = exact_duplicate_pairs(cases, normalized=False)
    normalized = exact_duplicate_pairs(cases, normalized=True)
    signature_exact = duplicate_pairs_by_signature(cases, normalized=False)
    signature_normalized = duplicate_pairs_by_signature(cases, normalized=True)
    near = near_duplicate_candidates(dedicated, mapping, root) if not mapping_missing else []
    near, near_review_summary = apply_near_duplicate_review(root, near, args.benchmark) if args.benchmark in {"task", "governance", "recovery", "idempotency"} else (near, {})
    refs = search_case_references(repo, cases)
    sem_issues = existing_semantic_task_id_issues(dedicated, mapping) if not mapping_missing else {}
    summary = benchmark_summary(dedicated, mapping) if not mapping_missing else {}
    cov = coverage(dedicated, mapping) if not mapping_missing else {}

    gold_counts = Counter(mapping[item["case"]["caseId"]]["goldSourceType"] for item in dedicated if item["case"]["caseId"] in mapping)
    review_counts = Counter(mapping[item["case"]["caseId"]]["reviewStatus"] for item in cases if item["case"]["caseId"] in mapping)
    historical_human_flags = sum(item["case"].get("humanReviewed") is True for item in cases)
    evidence_backed_human = sum(mapping[item["case"]["caseId"]].get("reviewStatus") == "HUMAN_REVIEWED" and mapping[item["case"]["caseId"]].get("humanReviewEvidencePresent") for item in cases if item["case"]["caseId"] in mapping)

    trace_overconstrained = [item["case"]["caseId"] for item in dedicated if item["case"].get("benchmarkType") == "TASK" and recursive_has_key(item["case"], "expectedToolCodes")]

    interpreter_path = repo / "shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/RuleBasedAgentTaskInterpreter.java"
    planner_path = repo / "shopops-admin/src/main/java/com/sirithree/shopops/admin/agent/service/impl/RulePlannerService.java"
    interpreter_text = interpreter_path.read_text(encoding="utf-8", errors="ignore")
    planner_text = planner_path.read_text(encoding="utf-8", errors="ignore")
    task_scenarios = sorted({item["case"].get("scenario") for item in dedicated if item["case"].get("benchmarkType") == "TASK"})
    reachability = {
        scenario: {"interpreter": f'"{scenario}"' in interpreter_text, "planner": f'"{scenario}"' in planner_text,
                   "reachable": f'"{scenario}"' in interpreter_text and f'"{scenario}"' in planner_text}
        for scenario in task_scenarios
    }

    result = {
        "auditVersion": "stage5-idempotency-dataset-audit-v1" if args.benchmark == "idempotency" else ("stage4-recovery-dataset-audit-v1" if args.benchmark == "recovery" else ("stage7b-governance-scaleup-audit-v1" if args.benchmark == "governance" else ("stage7a-task-scaleup-audit-v1" if args.benchmark == "task" else "stage1-dataset-audit-v1"))),
        "sourceManifest": mapping_doc.get("sourceDatasetManifest"),
        "allCaseCount": len(cases),
        "dedicatedCaseCount": len(dedicated),
        "resourceRoleCounts": dict(Counter(item["resourceRole"] for item in cases)),
        "schemaValidation": {"errorCount": len(schema), "errors": schema},
        "semanticRootMapping": {"mappingCaseCount": len(mapping), "missingCaseIds": mapping_missing, "unknownCaseIds": mapping_unknown},
        "benchmarks": summary,
        "dedicatedSemanticRootCount": len(root_groups(dedicated, mapping)) if not mapping_missing else None,
        "heldOutDedicatedCaseCount": sum(item["split"] == "test" for item in dedicated),
        "heldOutSemanticRootCount": sum(value.get("heldOutSemanticRootCount", 0) for value in summary.values()) if summary else None,
        "testExclusiveSemanticRootCount": sum(value.get("testExclusiveSemanticRootCount", 0) for value in summary.values()) if summary else None,
        "leakage": {
            "dedicatedCrossSplitSemanticRootCount": len(dedicated_leaks),
            "dedicatedCrossSplitSemanticRoots": dedicated_leaks,
            "allResourceCrossSplitSemanticRootCount": len(all_leaks),
            "crossSplitParentLeakCount": len(parent_leaks),
            "crossSplitParentLeaks": parent_leaks,
            "nearDuplicateCrossSplitCount": sum(item["crossSplit"] for item in near),
            **refs,
        },
        "duplicates": {
            "exactRuntimePayloadPairCount": len(exact),
            "exactRuntimePayloadPairs": exact,
            "normalizedRuntimePayloadPairCount": len(normalized),
            "normalizedRuntimePayloadPairs": normalized,
            "exactInputGoldSignaturePairCount": len(signature_exact),
            "exactInputGoldSignaturePairs": signature_exact,
            "normalizedInputGoldSignaturePairCount": len(signature_normalized),
            "normalizedInputGoldSignaturePairs": signature_normalized,
            "nearDuplicateCandidateCount": len(near),
            "nearDuplicateReviewSummary": near_review_summary,
            "nearDuplicateUnresolvedHighRiskCount": near_review_summary.get("unresolved", 0) if near_review_summary else None,
            "nearDuplicateCandidates": near,
        },
        "legacySemanticTaskIdAudit": sem_issues,
        "goldProvenance": {
            "dedicatedCounts": dict(gold_counts),
            "directSelfReferenceEvidenceCount": 0,
            "legacyMigratedProvenanceUnverifiableCount": gold_counts.get("LEGACY_MIGRATED", 0),
            "traceOverconstrainedGoldCount": len(trace_overconstrained),
            "traceOverconstrainedCaseIds": trace_overconstrained,
        },
        "reviewTruth": {
            "historicalHumanReviewedTrueFlags": historical_human_flags,
            "evidenceBackedHumanReviewed": evidence_backed_human,
            "currentAuditReviewStatusCounts": dict(review_counts),
            "reviewEvidenceArtifactsFound": 0,
        },
        "taskCapabilityReachability": reachability,
        "coverage": cov,
    }

    output = repo / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    label = "STAGE5_IDEMPOTENCY_DATASET_AUDIT" if args.benchmark == "idempotency" else ("STAGE4_RECOVERY_DATASET_AUDIT" if args.benchmark == "recovery" else ("STAGE7B_GOVERNANCE_SCALEUP_AUDIT" if args.benchmark == "governance" else ("STAGE7A_TASK_SCALEUP_AUDIT" if args.benchmark == "task" else "STAGE1_DATASET_AUDIT")))
    print(f"{label} cases={len(cases)} dedicated={len(dedicated)} roots={result['dedicatedSemanticRootCount']}")
    print(f"SCHEMA_ERRORS={len(schema)} MAPPING_MISSING={len(mapping_missing)} MAPPING_UNKNOWN={len(mapping_unknown)}")
    print(f"CROSS_SPLIT_ROOT_LEAKS={len(dedicated_leaks)} NEAR_DUPLICATE_CANDIDATES={len(near)}")
    print(f"EXACT_DUPLICATE_PAIRS={len(exact)} NORMALIZED_DUPLICATE_PAIRS={len(normalized)} INPUT_GOLD_EXACT={len(signature_exact)} INPUT_GOLD_NORMALIZED={len(signature_normalized)}")
    print(f"HUMAN_REVIEW_EVIDENCE_BACKED={evidence_backed_human} HISTORICAL_TRUE_FLAGS={historical_human_flags}")
    try:
        shown_output = output.relative_to(repo)
    except ValueError:
        shown_output = output
    print(f"OUTPUT={shown_output}")

    structural_failures = bool(schema or mapping_missing or mapping_unknown or refs["productionCaseIdReferences"] or refs["heldOutInputEvaluatorReferences"])
    if args.check and structural_failures:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
