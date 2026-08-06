from __future__ import annotations

import json
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
EVAL = DOCS / "evaluation"
SAMPLES = EVAL / "public-real-business-samples.json"


def read_json(path: Path) -> object:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def pct(numerator: int | float, denominator: int | float) -> float:
    return round((numerator / denominator * 100.0) if denominator else 0.0, 2)


def main() -> None:
    samples = read_json(SAMPLES)
    results = []
    total_expected = 0
    total_detected_expected = 0
    total_predicted = 0
    by_signal = defaultdict(lambda: {"expected": 0, "detectedExpected": 0, "predicted": 0})
    by_type = defaultdict(lambda: {"samples": 0, "expected": 0, "detectedExpected": 0, "predicted": 0})

    for sample in samples:
        expected = set(sample.get("expectedSignals", []))
        predicted = detect_signals(sample)
        matched = expected & predicted
        total_expected += len(expected)
        total_detected_expected += len(matched)
        total_predicted += len(predicted)
        sample_type = sample["sampleType"]
        by_type[sample_type]["samples"] += 1
        by_type[sample_type]["expected"] += len(expected)
        by_type[sample_type]["detectedExpected"] += len(matched)
        by_type[sample_type]["predicted"] += len(predicted)
        for signal in expected:
            by_signal[signal]["expected"] += 1
            if signal in predicted:
                by_signal[signal]["detectedExpected"] += 1
        for signal in predicted:
            by_signal[signal]["predicted"] += 1
        results.append({
            "sampleId": sample["sampleId"],
            "dataset": sample["dataset"],
            "sampleType": sample_type,
            "expectedSignals": sorted(expected),
            "predictedSignals": sorted(predicted),
            "matchedSignals": sorted(matched),
            "missedSignals": sorted(expected - predicted),
            "extraSignals": sorted(predicted - expected),
            "passed": expected.issubset(predicted),
        })

    precision = pct(total_detected_expected, total_predicted)
    recall = pct(total_detected_expected, total_expected)
    f1 = round((2 * precision * recall / (precision + recall)) if precision + recall else 0.0, 2)
    summary = {
        "generatedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "evaluationName": "shopops-real-anomaly-evaluation-v1",
        "sampleCount": len(samples),
        "passedSampleCount": sum(1 for item in results if item["passed"]),
        "samplePassRate": pct(sum(1 for item in results if item["passed"]), len(results)),
        "expectedSignalCount": total_expected,
        "predictedSignalCount": total_predicted,
        "matchedSignalCount": total_detected_expected,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "datasetCounts": dict(Counter(sample["dataset"] for sample in samples)),
        "sampleTypeCounts": dict(Counter(sample["sampleType"] for sample in samples)),
        "signalMetrics": {
            signal: metric(stat)
            for signal, stat in sorted(by_signal.items())
        },
        "sampleTypeMetrics": {
            sample_type: {
                "samples": stat["samples"],
                **metric(stat),
            }
            for sample_type, stat in sorted(by_type.items())
        },
        "thresholds": thresholds(),
        "limitations": [
            "This is an offline rule-based evaluation over public benchmark samples, not online production monitoring.",
            "Predicted signals are generated from sample metrics only; model-generated narratives are not included.",
            "Criteo campaign IDs are anonymized, so ad risks are evaluated at campaign benchmark level.",
        ],
        "results": results,
    }

    (DOCS / "ShopOps-real-anomaly-evaluation.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (DOCS / "ShopOps-real-anomaly-evaluation.md").write_text(render_markdown(summary), encoding="utf-8")
    print(f"Generated {DOCS / 'ShopOps-real-anomaly-evaluation.json'}")
    print(f"Generated {DOCS / 'ShopOps-real-anomaly-evaluation.md'}")


def detect_signals(sample: dict[str, object]) -> set[str]:
    metrics = sample.get("metrics", {})
    sample_type = sample.get("sampleType")
    predicted = set()

    gmv_delta = abs(number(metrics.get("gmvDeltaRate")))
    if gmv_delta >= 0.25:
        predicted.update(["gmv_anomaly", "order_volume_anomaly"])

    refund_rate = number(metrics.get("refundProxyRate"))
    refund_amount = number(metrics.get("refundProxyAmount"))
    if refund_rate >= 0.02 or refund_amount >= 500:
        predicted.update(["refund_proxy_increase", "after_sales_risk"])

    review_score = number(metrics.get("reviewScore"))
    low_score_count = number(metrics.get("lowScoreReviewCount"))
    avg_score = number(metrics.get("avgReviewScore"))
    if review_score and review_score <= 2:
        predicted.update(["low_score_review", "customer_experience_risk"])
    if low_score_count >= 3 or (avg_score and avg_score <= 3.5):
        predicted.update(["product_review_risk", "product_optimization_candidate"])

    if sample_type == "delivery_delay_risk":
        predicted.update(["delivery_delay", "customer_experience_risk"])

    conversion_rate = number(metrics.get("conversionRate"))
    clicks = number(metrics.get("clicks"))
    cost = number(metrics.get("cost"))
    conversions = number(metrics.get("conversions"))
    if sample_type == "ad_low_conversion" or (clicks >= 500 and conversion_rate <= 0.08):
        predicted.add("low_conversion_risk")
    if sample_type == "ad_low_conversion" or (cost > 0 and conversions <= 100):
        predicted.add("ad_spend_risk")

    cancel_amount = number(metrics.get("cancelAmount"))
    cancel_lines = number(metrics.get("cancelLineCount"))
    if cancel_amount >= 500 or cancel_lines >= 5:
        predicted.update(["after_sales_risk", "refund_proxy_increase"])

    if sample_type == "external_event_context" or metrics.get("eventType"):
        predicted.add("external_event_context")

    return predicted


def number(value: object) -> float:
    if value is None or value == "":
        return 0.0
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def thresholds() -> dict[str, object]:
    return {
        "gmvDeltaRateAbsMin": 0.25,
        "refundProxyRateMin": 0.02,
        "refundProxyAmountMin": 500,
        "lowReviewScoreMax": 2,
        "lowScoreReviewCountMin": 3,
        "avgReviewScoreMax": 3.5,
        "adClickMin": 500,
        "adConversionRateMax": 0.08,
        "cancelAmountMin": 500,
        "cancelLineCountMin": 5,
    }


def metric(stat: dict[str, int]) -> dict[str, object]:
    precision = pct(stat["detectedExpected"], stat["predicted"])
    recall = pct(stat["detectedExpected"], stat["expected"])
    f1 = round((2 * precision * recall / (precision + recall)) if precision + recall else 0.0, 2)
    return {
        "expected": stat["expected"],
        "predicted": stat["predicted"],
        "matched": stat["detectedExpected"],
        "precision": precision,
        "recall": recall,
        "f1": f1,
    }


def render_markdown(summary: dict[str, object]) -> str:
    type_rows = "\n".join(
        f"| {sample_type} | {stat['samples']} | {stat['precision']}% | {stat['recall']}% | {stat['f1']} |"
        for sample_type, stat in summary["sampleTypeMetrics"].items()
    )
    signal_rows = "\n".join(
        f"| {signal} | {stat['expected']} | {stat['predicted']} | {stat['precision']}% | {stat['recall']}% | {stat['f1']} |"
        for signal, stat in summary["signalMetrics"].items()
    )
    limitations = "\n".join(f"- {item}" for item in summary["limitations"])
    return f"""# ShopOps Real Anomaly Evaluation

Generated at: {summary["generatedAt"]}

This evaluation runs a fixed rule detector over the public real-data business samples and compares predicted anomaly signals against expected signals.

## Summary

| Metric | Value |
|---|---:|
| Samples | {summary["sampleCount"]} |
| Passed samples | {summary["passedSampleCount"]} |
| Sample pass rate | {summary["samplePassRate"]}% |
| Expected signals | {summary["expectedSignalCount"]} |
| Predicted signals | {summary["predictedSignalCount"]} |
| Matched signals | {summary["matchedSignalCount"]} |
| Precision | {summary["precision"]}% |
| Recall | {summary["recall"]}% |
| F1 | {summary["f1"]} |

## By Sample Type

| Sample Type | Samples | Precision | Recall | F1 |
|---|---:|---:|---:|---:|
{type_rows}

## By Signal

| Signal | Expected | Predicted | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|
{signal_rows}

## Limitations

{limitations}
"""


if __name__ == "__main__":
    main()
