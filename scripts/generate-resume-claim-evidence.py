from __future__ import annotations

import json
import shutil
import zipfile
from collections import Counter
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
EVAL_DIR = DOCS / "evaluation"
TARGET_EVAL = ROOT / "shopops-admin" / "target" / "evaluation" / "agent-eval-portfolio-summary.json"
PUBLIC_BASELINE = DOCS / "ShopOps-public-real-baseline.json"
PUBLIC_SAMPLES = EVAL_DIR / "public-real-business-samples.json"
EXCEL_EXPORT_DIR = ROOT / "shopops-admin" / "target" / "shopops-exports"
EXCEL_EVIDENCE_FILE = EVAL_DIR / "shopops-operation-report-sample.xlsx"
TIMING_EVIDENCE = DOCS / "ShopOps-operation-timing-evidence.json"


def read_json(path: Path) -> object:
    if not path.exists():
        raise FileNotFoundError(f"Missing required file: {path}")
    return json.loads(path.read_text(encoding="utf-8-sig"))


def pct(numerator: int | float, denominator: int | float) -> float:
    return round((numerator / denominator * 100.0) if denominator else 0.0, 2)


def main() -> None:
    DOCS.mkdir(parents=True, exist_ok=True)
    baseline = read_json(PUBLIC_BASELINE)
    samples = read_json(PUBLIC_SAMPLES)
    agent_eval = read_json(TARGET_EVAL) if TARGET_EVAL.exists() else None
    timing_evidence = read_json(TIMING_EVIDENCE) if TIMING_EVIDENCE.exists() else None

    sample_types = Counter(sample["sampleType"] for sample in samples)
    tool_counts = Counter()
    high_risk_tools = {"order.refund_execute", "product.update_title", "ad.suggest_budget"}
    for sample in samples:
        tool_counts.update(sample["recommendedTools"])

    ad_samples = [sample for sample in samples if sample["sampleType"] == "ad_low_conversion"]
    ad_detected = [
        sample for sample in ad_samples
        if "ad.query_performance" in sample["recommendedTools"]
        and "ad.suggest_budget" in sample["recommendedTools"]
    ]

    generated_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    claims = []

    claims.append(verified(
        "public_real_data_scale",
        "公开真实数据规模",
        {
            "olistOrders": baseline["realOrderCount"],
            "olistReviews": baseline["realReviewCount"],
            "olistProducts": baseline["realProductCount"],
            "criteoImpressions": baseline["criteoImpressionCount"],
            "criteoClicks": baseline["criteoClickCount"],
            "criteoConversions": baseline["criteoConversionCount"],
            "uciRetailLines": baseline["onlineRetailLineCount"],
            "storeHolidayEvents": baseline["storeHolidayEventCount"],
        },
        "读取本地公开数据集并全量聚合。Criteo TSV 为全量扫描，UCI xlsx 使用只读模式遍历。",
        [str(PUBLIC_BASELINE.relative_to(ROOT)).replace("\\", "/")],
    ))

    claims.append(verified(
        "real_business_sample_count",
        "真实数据业务样例数",
        len(samples),
        "由 Olist、Criteo、UCI Online Retail、Store Sales 样例合并生成。",
        [
            str(PUBLIC_SAMPLES.relative_to(ROOT)).replace("\\", "/"),
            str(PUBLIC_BASELINE.relative_to(ROOT)).replace("\\", "/"),
        ],
    ))

    claims.append(verified(
        "derived_mcp_tool_calls",
        "派生 MCP 工具调用数",
        sum(tool_counts.values()),
        "按每个真实数据业务样例的 recommendedTools 聚合，代表评测链路规划出的工具调用量。",
        [str(PUBLIC_SAMPLES.relative_to(ROOT)).replace("\\", "/")],
        extra={"toolCallCounts": dict(tool_counts)},
    ))

    claims.append(verified(
        "high_risk_approval_routed_calls",
        "高风险动作审批路由数",
        sum(tool_counts[tool] for tool in high_risk_tools),
        "统计真实样例链路中 order.refund_execute、product.update_title、ad.suggest_budget 三类高风险工具路由。",
        [str(PUBLIC_SAMPLES.relative_to(ROOT)).replace("\\", "/")],
    ))

    claims.append(verified(
        "low_roi_ad_detection_proxy",
        "低转化/低 ROI 投放识别效果",
        {
            "labeledAdRiskSamples": len(ad_samples),
            "routedAdRiskSamples": len(ad_detected),
            "routingRecall": pct(len(ad_detected), len(ad_samples)),
        },
        "基于 Criteo 真实广告曝光、点击、转化、成本聚合出广告风险样例，并验证样例均被路由到广告表现查询和预算建议工具。该指标是工具路由召回，不等同于线上 ROI 模型精度。",
        [str(PUBLIC_SAMPLES.relative_to(ROOT)).replace("\\", "/")],
    ))

    if agent_eval is not None:
        claims.append(verified(
            "agent_evaluation_suite",
            "Agent 自动化评测套件",
            {
                "caseCount": agent_eval["caseCount"],
                "passedCaseCount": agent_eval["passedCaseCount"],
                "toolInvocationSuccessRate": agent_eval["toolInvocationSuccessRate"],
                "approvalDecisionAccuracy": agent_eval["approvalDecisionAccuracy"],
                "configEffectAccuracy": agent_eval["configEffectAccuracy"],
                "avgTaskDurationMs": agent_eval["avgTaskDurationMs"],
            },
            "由 Maven 集成测试生成 target/evaluation/agent-eval-portfolio-summary.json。",
            [str(TARGET_EVAL.relative_to(ROOT)).replace("\\", "/")],
        ))
    else:
        claims.append(not_verified(
            "agent_evaluation_suite",
            "Agent 自动化评测套件",
            "缺少 shopops-admin/target/evaluation/agent-eval-portfolio-summary.json；需要先执行 scripts/run-agent-evaluation.ps1。",
        ))

    excel_export = latest_valid_xlsx()
    if excel_export is None:
        claims.append(not_verified(
            "excel_real_xlsx_export",
            "真实 Excel xlsx 文件导出",
            "缺少可校验的 shopops-admin/target/shopops-exports/*.xlsx；需要先执行 McpToolCatalogIntegrationTest 或通过工具网关调用 report.export_excel。",
            required="mvn -pl shopops-admin \"-Dtest=McpToolCatalogIntegrationTest\" test",
        ))
    else:
        shutil.copy2(excel_export, EXCEL_EVIDENCE_FILE)
        claims.append(verified(
            "excel_real_xlsx_export",
            "真实 Excel xlsx 文件导出",
            {
                "filePath": str(EXCEL_EVIDENCE_FILE.relative_to(ROOT)).replace("\\", "/"),
                "sourceFilePath": str(excel_export.relative_to(ROOT)).replace("\\", "/"),
                "fileSizeBytes": EXCEL_EVIDENCE_FILE.stat().st_size,
                "worksheetCount": 4,
            },
            "通过工具网关调用 report.export_excel 生成本地 .xlsx 文件，并用 ZipFile 校验 workbook 与 4 个 worksheet XML。",
            [
                str(EXCEL_EVIDENCE_FILE.relative_to(ROOT)).replace("\\", "/"),
                "shopops-admin/src/main/java/com/sirithree/shopops/admin/tool/executor/ReportExportExcelExecutor.java",
                "shopops-admin/src/test/java/com/sirithree/shopops/admin/tool/McpToolCatalogIntegrationTest.java",
            ],
        ))

    claims.extend([
        not_verified(
            "manual_35min_to_agent_4min",
            "单次日报从 35 分钟降到 4 分钟",
            "当前没有至少 5 次人工手工流程计时记录，不能把机器侧耗时换算成运营人员节省时间。",
            required="设计人工基线实验：固定任务、固定输入表、记录至少 5 次手工整理耗时；同时记录同一任务的 Agent 端到端耗时。",
            timingEvidence=timing_evidence.get("timeSavingSummary") if timing_evidence else "Run scripts/generate-operation-timing-evidence.py first.",
        ),
        not_verified(
            "old_120_simulated_tasks",
            "120 个模拟运营任务",
            "当前真实基线是 760 个公开真实数据业务样例；旧的 120 模拟任务数字没有对应现存复跑产物，不建议继续使用。",
            replacement="使用 public_real_data_scale + real_business_sample_count。",
        ),
        not_verified(
            "old_462_tool_calls",
            "462 次工具调用",
            "当前可复跑的公开真实基线为 2670 次派生 MCP 工具调用；旧 462 没有对应现存复跑产物。",
            replacement="使用 derived_mcp_tool_calls。",
        ),
        not_verified(
            "old_96_8_tool_success_rate",
            "96.8% 工具调用成功率",
            "当前 Maven 自动化评测真实产物中的工具调用成功率为 98.6%；旧 96.8% 不建议继续使用。",
            replacement="使用 agent_evaluation_suite.toolInvocationSuccessRate。",
        ),
        not_verified(
            "feishu_real_sync_success_rate",
            "飞书同步真实成功率",
            "当前 feishu.sync_report 是 demo connector，返回 feishu.example.com，不是飞书开放平台真实 API 调用。",
            required="接入飞书开放平台 app_id/app_secret 或 webhook，记录真实 API 响应和失败重试日志。",
        ),
        not_verified(
            "excel_real_export_time_saving",
            "Excel 导出真实耗时收益",
            "当前已经能生成真实 xlsx 文件，但还没有人工整理 Excel 的计时对比，因此不能写“节省多少时间”。",
            required="记录同一份运营日报人工整理 Excel 的耗时，并与 report.export_excel 的接口耗时/文件生成耗时对比。",
            timingEvidence=timing_evidence.get("manualSummary") if timing_evidence else "Run scripts/generate-operation-timing-evidence.py first.",
        ),
        not_verified(
            "anomaly_recall_88_5",
            "异常指标召回率 88.5%",
            "旧 88.5% 没有独立预测结果与真实标签对比产物。当前可以引用 Criteo 广告风险工具路由召回，但不能写成异常检测召回率。",
            replacement="使用 low_roi_ad_detection_proxy.routingRecall，并明确称为工具路由召回。",
        ),
    ])

    summary = {
        "generatedAt": generated_at,
        "evidenceName": "shopops-resume-claim-evidence-v1",
        "claimCount": len(claims),
        "verifiedClaimCount": sum(1 for claim in claims if claim["status"] == "VERIFIED"),
        "notVerifiedClaimCount": sum(1 for claim in claims if claim["status"] == "NOT_VERIFIED"),
        "sampleTypeCounts": dict(sample_types),
        "claims": claims,
    }

    json_path = DOCS / "ShopOps-resume-claim-evidence.json"
    md_path = DOCS / "ShopOps-resume-claim-evidence.md"
    json_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    md_path.write_text(render_markdown(summary), encoding="utf-8")
    print(f"Generated {json_path}")
    print(f"Generated {md_path}")


def verified(key: str, title: str, value: object, how: str, sources: list[str], extra: dict[str, object] | None = None) -> dict[str, object]:
    item = {
        "key": key,
        "title": title,
        "status": "VERIFIED",
        "value": value,
        "howMeasured": how,
        "sources": sources,
    }
    if extra:
        item.update(extra)
    return item


def not_verified(key: str, title: str, reason: str, **extra: object) -> dict[str, object]:
    item = {
        "key": key,
        "title": title,
        "status": "NOT_VERIFIED",
        "reason": reason,
    }
    item.update(extra)
    return item


def latest_valid_xlsx() -> Path | None:
    if not EXCEL_EXPORT_DIR.exists():
        return None
    files = sorted(EXCEL_EXPORT_DIR.glob("*.xlsx"), key=lambda path: path.stat().st_mtime, reverse=True)
    for path in files:
        try:
            with zipfile.ZipFile(path) as workbook:
                required = [
                    "xl/workbook.xml",
                    "xl/worksheets/sheet1.xml",
                    "xl/worksheets/sheet2.xml",
                    "xl/worksheets/sheet3.xml",
                    "xl/worksheets/sheet4.xml",
                ]
                if all(workbook.getinfo(name) for name in required):
                    return path
        except (KeyError, zipfile.BadZipFile):
            continue
    return None


def render_markdown(summary: dict[str, object]) -> str:
    lines = [
        "# ShopOps Resume Claim Evidence",
        "",
        f"Generated at: {summary['generatedAt']}",
        "",
        "This file separates resume-safe metrics from metrics that still need real experiments or third-party API integration.",
        "",
        "## Summary",
        "",
        "| Metric | Value |",
        "|---|---:|",
        f"| Total claims | {summary['claimCount']} |",
        f"| Verified claims | {summary['verifiedClaimCount']} |",
        f"| Not verified claims | {summary['notVerifiedClaimCount']} |",
        "",
        "## Claims",
        "",
        "| Key | Status | Value / Reason |",
        "|---|---|---|",
    ]
    for claim in summary["claims"]:
        if claim["status"] == "VERIFIED":
            value = json.dumps(claim["value"], ensure_ascii=False)
        else:
            value = claim["reason"]
        lines.append(f"| {claim['key']} | {claim['status']} | {value} |")
    lines.extend([
        "",
        "## Resume-Safe Wording",
        "",
        "```latex",
        r"\resumeItem{基于 \textbf{Olist、Criteo Attribution、UCI Online Retail 和 Store Sales} 等公开真实数据集构建多源运营评测基线，覆盖订单异常、低分评价、商品评价风险、广告低转化、退款/取消代理和外部事件等场景；从 \textbf{99441 笔订单、99224 条评价、32951 个商品、16468027 次广告曝光、5947563 次点击、806196 次转化和 541909 行零售交易} 中生成 \textbf{760 个真实数据业务样例}。}",
        "",
        r"\resumeItem{将真实数据样例映射到 \textbf{18 个 MCP 工具}的 Agent 执行链路，派生 \textbf{2670 次工具调用}，其中 \textbf{450 次}退款执行、商品标题修改和广告预算建议等高风险动作进入审批路由；自动化评测套件当前 \textbf{14/14} 个 Agent 链路用例通过，工具调用成功率达到 \textbf{98.6\%}，审批决策和配置生效率均为 \textbf{100\%}。}",
        "```",
        "",
        "Do not claim the old 35-to-4-minute, 88.5% anomaly recall, real Feishu sync, or real Excel time-saving metrics until the required experiments are implemented and recorded.",
    ])
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    main()
