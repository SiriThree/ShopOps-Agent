from __future__ import annotations

import csv
import json
import statistics
import zipfile
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
EVAL = DOCS / "evaluation"
AGENT_EVAL_SUMMARY = ROOT / "shopops-admin" / "target" / "evaluation" / "agent-eval-portfolio-summary.json"
DEMO_SUMMARY = ROOT / "shopops-admin" / "target" / "demo" / "olist-agentops-demo-summary.json"
EXCEL_EVIDENCE = EVAL / "shopops-operation-report-sample.xlsx"
MANUAL_TIMING = EVAL / "manual-report-timing.csv"
MANUAL_TEMPLATE = EVAL / "manual-report-timing-template.csv"
ESTIMATED_TIMING = EVAL / "manual-report-timing-estimated.csv"


def read_json(path: Path) -> dict[str, object] | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def percentile(values: list[float], ratio: float) -> float:
    if not values:
        return 0.0
    values = sorted(values)
    index = min(len(values) - 1, max(0, round((len(values) - 1) * ratio)))
    return round(values[index], 2)


def main() -> None:
    EVAL.mkdir(parents=True, exist_ok=True)
    ensure_manual_template()

    agent_eval = read_json(AGENT_EVAL_SUMMARY)
    demo_summary = read_json(DEMO_SUMMARY)
    excel = excel_summary()
    manual = manual_summary()
    estimated = estimated_summary()

    durations = []
    if agent_eval:
        for result in agent_eval.get("results", []):
            duration = result.get("durationMs")
            if isinstance(duration, (int, float)):
                durations.append(float(duration))

    machine_summary = {
        "agentEvalCaseCount": agent_eval.get("caseCount") if agent_eval else 0,
        "agentEvalPassedCaseCount": agent_eval.get("passedCaseCount") if agent_eval else 0,
        "agentEvalAvgDurationMs": round(float(agent_eval.get("avgTaskDurationMs", 0)), 2) if agent_eval else 0,
        "agentEvalMinDurationMs": round(min(durations), 2) if durations else 0,
        "agentEvalP50DurationMs": percentile(durations, 0.5),
        "agentEvalP95DurationMs": percentile(durations, 0.95),
        "agentEvalMaxDurationMs": round(max(durations), 2) if durations else 0,
        "olistDemoTaskDurationMs": demo_summary.get("task", {}).get("durationMs") if demo_summary else None,
        "excelExportFileSizeBytes": excel["fileSizeBytes"],
        "excelExportWorksheetCount": excel["worksheetCount"],
        "excelExportEvidenceFile": excel["filePath"],
    }

    agent_minutes = (
        float(machine_summary["olistDemoTaskDurationMs"]) / 60000.0
        if machine_summary["olistDemoTaskDurationMs"] is not None
        else None
    )
    claimable_time_saving = manual["recordCount"] >= 5 and agent_minutes is not None
    if claimable_time_saving:
        manual_minutes = manual["avgManualMinutes"]
        saved_minutes = round(manual_minutes - agent_minutes, 2)
        reduction_rate = round((saved_minutes / manual_minutes * 100.0) if manual_minutes else 0.0, 2)
    else:
        manual_minutes = manual["avgManualMinutes"] if manual["recordCount"] else None
        saved_minutes = None
        reduction_rate = None

    summary = {
        "generatedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "evidenceName": "shopops-operation-timing-evidence-v1",
        "machineTimingVerified": bool(agent_eval),
        "excelExportVerified": excel["verified"],
        "manualTimingRecordCount": manual["recordCount"],
        "timeSavingClaimStatus": "VERIFIED" if claimable_time_saving else "NOT_VERIFIED",
        "machineSummary": machine_summary,
        "manualSummary": manual,
        "estimatedTimingSummary": estimated,
        "timeSavingSummary": {
            "manualAvgMinutes": manual_minutes,
            "agentDemoMinutes": round(agent_minutes, 4) if agent_minutes is not None else None,
            "savedMinutes": saved_minutes,
            "reductionRatePercent": reduction_rate,
            "claimable": claimable_time_saving,
            "requirement": "Need at least 5 manual timing records in docs/evaluation/manual-report-timing.csv and a fresh Olist demo summary.",
        },
        "sources": [
            relative(AGENT_EVAL_SUMMARY),
            relative(DEMO_SUMMARY),
            relative(EXCEL_EVIDENCE),
            relative(MANUAL_TIMING),
            relative(ESTIMATED_TIMING),
        ],
    }

    write_json(DOCS / "ShopOps-operation-timing-evidence.json", summary)
    (DOCS / "ShopOps-operation-timing-evidence.md").write_text(render_markdown(summary), encoding="utf-8")
    print(f"Generated {DOCS / 'ShopOps-operation-timing-evidence.json'}")
    print(f"Generated {DOCS / 'ShopOps-operation-timing-evidence.md'}")
    print(f"Manual timing template: {MANUAL_TEMPLATE}")


def ensure_manual_template() -> None:
    if not MANUAL_TEMPLATE.exists():
        rows = [
            ["runId", "operator", "dataset", "task", "startedAt", "finishedAt", "manualMinutes", "notes"],
            ["manual-001", "your-name", "olist-2018-08-07", "daily_review_excel_report", "", "", "", "Fill after timing the manual workflow."],
        ]
        with MANUAL_TEMPLATE.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle)
            writer.writerows(rows)
    if not MANUAL_TIMING.exists():
        with MANUAL_TIMING.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle)
            writer.writerow(["runId", "operator", "dataset", "task", "startedAt", "finishedAt", "manualMinutes", "notes"])


def manual_summary() -> dict[str, object]:
    records = []
    if MANUAL_TIMING.exists():
        with MANUAL_TIMING.open(encoding="utf-8-sig", newline="") as handle:
            for row in csv.DictReader(handle):
                value = row.get("manualMinutes", "").strip()
                if not value:
                    continue
                records.append(float(value))
    return {
        "recordCount": len(records),
        "avgManualMinutes": round(statistics.mean(records), 2) if records else 0,
        "minManualMinutes": round(min(records), 2) if records else 0,
        "maxManualMinutes": round(max(records), 2) if records else 0,
        "source": relative(MANUAL_TIMING),
        "template": relative(MANUAL_TEMPLATE),
    }


def estimated_summary() -> dict[str, object]:
    manual_values = []
    agent_values = []
    rows = 0
    if ESTIMATED_TIMING.exists():
        with ESTIMATED_TIMING.open(encoding="utf-8-sig", newline="") as handle:
            for row in csv.DictReader(handle):
                manual = row.get("manualMinutes", "").strip()
                agent = row.get("agentAssistedMinutes", "").strip()
                if not manual or not agent:
                    continue
                rows += 1
                manual_values.append(float(manual))
                agent_values.append(float(agent))
    manual_avg = round(statistics.mean(manual_values), 2) if manual_values else 0
    agent_avg = round(statistics.mean(agent_values), 2) if agent_values else 0
    saved = round(manual_avg - agent_avg, 2) if rows else 0
    reduction = round((saved / manual_avg * 100.0) if manual_avg else 0.0, 2)
    return {
        "status": "ESTIMATED" if rows >= 5 else "MISSING",
        "recordCount": rows,
        "estimatedManualAvgMinutes": manual_avg,
        "estimatedAgentAssistedAvgMinutes": agent_avg,
        "estimatedSavedMinutes": saved,
        "estimatedReductionRatePercent": reduction,
        "source": relative(ESTIMATED_TIMING),
        "claimBoundary": "Estimated from fixed ecommerce operation workflow steps. Do not present as measured human timing.",
    }


def excel_summary() -> dict[str, object]:
    if not EXCEL_EVIDENCE.exists():
        return {
            "verified": False,
            "filePath": relative(EXCEL_EVIDENCE),
            "fileSizeBytes": 0,
            "worksheetCount": 0,
        }
    worksheet_count = 0
    verified = False
    try:
        with zipfile.ZipFile(EXCEL_EVIDENCE) as workbook:
            worksheet_count = sum(1 for name in workbook.namelist() if name.startswith("xl/worksheets/sheet"))
            verified = workbook.getinfo("xl/workbook.xml") is not None and worksheet_count >= 1
    except (KeyError, zipfile.BadZipFile):
        verified = False
    return {
        "verified": verified,
        "filePath": relative(EXCEL_EVIDENCE),
        "fileSizeBytes": EXCEL_EVIDENCE.stat().st_size,
        "worksheetCount": worksheet_count,
    }


def render_markdown(summary: dict[str, object]) -> str:
    machine = summary["machineSummary"]
    manual = summary["manualSummary"]
    estimated = summary["estimatedTimingSummary"]
    saving = summary["timeSavingSummary"]
    return f"""# ShopOps Operation Timing Evidence

Generated at: {summary["generatedAt"]}

This report records machine-side timing evidence and keeps manual time-saving claims separate until manual timing records exist.

## Machine Timing

| Metric | Value |
|---|---:|
| Agent evaluation cases | {machine["agentEvalCaseCount"]} |
| Passed cases | {machine["agentEvalPassedCaseCount"]} |
| Avg evaluation case duration | {machine["agentEvalAvgDurationMs"]} ms |
| P50 evaluation case duration | {machine["agentEvalP50DurationMs"]} ms |
| P95 evaluation case duration | {machine["agentEvalP95DurationMs"]} ms |
| Olist demo task duration | {machine["olistDemoTaskDurationMs"]} ms |
| Excel evidence file size | {machine["excelExportFileSizeBytes"]} bytes |
| Excel worksheet count | {machine["excelExportWorksheetCount"]} |

## Manual Timing

| Metric | Value |
|---|---:|
| Manual timing records | {manual["recordCount"]} |
| Avg manual minutes | {manual["avgManualMinutes"]} |
| Min manual minutes | {manual["minManualMinutes"]} |
| Max manual minutes | {manual["maxManualMinutes"]} |

Manual timing template: `{manual["template"]}`

## Estimated Workflow Baseline

| Metric | Value |
|---|---:|
| Estimate status | {estimated["status"]} |
| Estimated records | {estimated["recordCount"]} |
| Estimated manual avg minutes | {estimated["estimatedManualAvgMinutes"]} |
| Estimated Agent-assisted avg minutes | {estimated["estimatedAgentAssistedAvgMinutes"]} |
| Estimated saved minutes | {estimated["estimatedSavedMinutes"]} |
| Estimated reduction rate | {estimated["estimatedReductionRatePercent"]}% |

Source: `{estimated["source"]}`

Boundary: {estimated["claimBoundary"]}

## Time-Saving Claim

| Metric | Value |
|---|---:|
| Claim status | {summary["timeSavingClaimStatus"]} |
| Manual avg minutes | {display(saving["manualAvgMinutes"])} |
| Agent demo minutes | {display(saving["agentDemoMinutes"])} |
| Saved minutes | {display(saving["savedMinutes"])} |
| Reduction rate | {display(saving["reductionRatePercent"], suffix="%")} |

Do not claim a measured manual time-saving number until `docs/evaluation/manual-report-timing.csv` contains at least 5 measured manual runs. Estimated workflow numbers may be used only when clearly labeled as estimates.
"""


def relative(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT)).replace("\\", "/")
    except ValueError:
        return str(path).replace("\\", "/")


def display(value: object, suffix: str = "") -> str:
    if value is None:
        return "N/A"
    return f"{value}{suffix}"


if __name__ == "__main__":
    main()
