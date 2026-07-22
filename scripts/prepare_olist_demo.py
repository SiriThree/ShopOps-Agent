#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate ShopOps demo JSON files from the Olist dataset.")
    parser.add_argument("--data-dir", default="data/archive", help="Directory containing Olist CSV files.")
    parser.add_argument("--output-dir", default="docs/demo-data/olist", help="Directory to write generated JSON files.")
    parser.add_argument("--date", default="2018-08-07", help="Target business date in YYYY-MM-DD format.")
    parser.add_argument("--tenant-id", type=int, default=1)
    parser.add_argument("--shop-id", type=int, default=1)
    parser.add_argument("--min-star", type=int, default=3, help="Negative comment threshold for connector metadata.")
    return parser.parse_args()


def decimal(value: str | int | float | Decimal | None) -> Decimal:
    if value in (None, ""):
        return Decimal("0")
    if isinstance(value, Decimal):
        return value
    return Decimal(str(value))


def quantize(value: Decimal, scale: str = "0.0001") -> Decimal:
    return value.quantize(Decimal(scale), rounding=ROUND_HALF_UP)


def pct(numerator: Decimal, denominator: Decimal) -> Decimal:
    if denominator == 0:
        return Decimal("0")
    return numerator / denominator


def fmt_number(value: Decimal, scale: str = "0.01") -> float:
    return float(value.quantize(Decimal(scale), rounding=ROUND_HALF_UP))


def fmt_ratio(value: Decimal) -> float:
    return float(quantize(value))


def short_product_name(category: str, product_id: str) -> str:
    clean = (category or "unknown").replace("_", " ").strip()
    title = " ".join(word.capitalize() for word in clean.split())
    return f"{title} / {product_id[:8]}"


def normalize_text(text: str) -> str:
    return " ".join((text or "").replace("\n", " ").replace("\r", " ").split())


def detect_risk_keywords(message: str) -> list[str]:
    lower = normalize_text(message).lower()
    labels: list[str] = []
    rules = [
        ("退款/退货", ("devol", "reemb", "cancel", "troca")),
        ("物流慢", ("atras", "demor", "prazo", "entrega")),
        ("描述不符", ("descr", "anuncio", "anúncio", "foto", "difer")),
        ("商品破损/质量问题", ("quebr", "defeit", "danific", "avari", "nao funciona", "não funciona")),
        ("缺件", ("falt", "incomplet")),
        ("尺寸问题", ("tamanho",)),
    ]
    for label, patterns in rules:
        if any(pattern in lower for pattern in patterns):
            labels.append(label)
    return labels or ["评价风险"]


@dataclass
class OrderInfo:
    purchase_date: str
    status: str
    payment_value: Decimal


def load_translation_map(data_dir: Path) -> dict[str, str]:
    path = data_dir / "product_category_name_translation.csv"
    translations: dict[str, str] = {}
    with path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            translations[row["product_category_name"]] = row["product_category_name_english"]
    return translations


def load_product_map(data_dir: Path, translations: dict[str, str]) -> dict[str, str]:
    path = data_dir / "olist_products_dataset.csv"
    products: dict[str, str] = {}
    with path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            category = row.get("product_category_name") or ""
            english = translations.get(category, category or "unknown")
            products[row["product_id"]] = short_product_name(english, row["product_id"])
    return products


def load_payments(data_dir: Path) -> dict[str, Decimal]:
    path = data_dir / "olist_order_payments_dataset.csv"
    payments: dict[str, Decimal] = defaultdict(lambda: Decimal("0"))
    with path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            payments[row["order_id"]] += decimal(row["payment_value"])
    return dict(payments)


def load_orders(data_dir: Path, payments: dict[str, Decimal]) -> dict[str, OrderInfo]:
    path = data_dir / "olist_orders_dataset.csv"
    orders: dict[str, OrderInfo] = {}
    with path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            purchase_date = row["order_purchase_timestamp"][:10]
            orders[row["order_id"]] = OrderInfo(
                purchase_date=purchase_date,
                status=row["order_status"],
                payment_value=payments.get(row["order_id"], Decimal("0")),
            )
    return orders


def aggregate_daily_orders(orders: dict[str, OrderInfo]) -> dict[str, dict[str, Decimal | int]]:
    daily: dict[str, dict[str, Decimal | int]] = defaultdict(
        lambda: {"gmv": Decimal("0"), "refundAmount": Decimal("0"), "orderCount": 0}
    )
    for info in orders.values():
        bucket = daily[info.purchase_date]
        bucket["gmv"] += info.payment_value
        bucket["orderCount"] += 1
        if info.status in {"canceled", "unavailable"}:
            bucket["refundAmount"] += info.payment_value
    return dict(daily)


def build_order_item_views(
    data_dir: Path, orders: dict[str, OrderInfo], product_names: dict[str, str], target_date: str
) -> tuple[dict[str, dict[str, object]], dict[str, dict[str, Decimal | int]]]:
    path = data_dir / "olist_order_items_dataset.csv"
    primary_item_by_order: dict[str, dict[str, object]] = {}
    product_sales: dict[str, dict[str, Decimal | int]] = defaultdict(
        lambda: {"salesQuantity": 0, "revenue": Decimal("0"), "freight": Decimal("0")}
    )
    with path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            order_id = row["order_id"]
            product_id = row["product_id"]
            price = decimal(row["price"])
            freight = decimal(row["freight_value"])
            display_name = product_names.get(product_id, short_product_name("unknown", product_id))
            current = primary_item_by_order.get(order_id)
            candidate = {
                "productId": product_id,
                "productName": display_name,
                "price": price,
            }
            if current is None or price > current["price"]:
                primary_item_by_order[order_id] = candidate
            order_info = orders.get(order_id)
            if order_info and order_info.purchase_date == target_date:
                stats = product_sales[product_id]
                stats["salesQuantity"] += 1
                stats["revenue"] += price
                stats["freight"] += freight
    return primary_item_by_order, product_sales


def build_order_summary_record(
    target: date, daily_orders: dict[str, dict[str, Decimal | int]], tenant_id: int, shop_id: int
) -> dict[str, object]:
    today = daily_orders.get(target.isoformat(), {"gmv": Decimal("0"), "refundAmount": Decimal("0"), "orderCount": 0})
    yesterday = daily_orders.get((target - timedelta(days=1)).isoformat(), {"gmv": Decimal("0"), "refundAmount": Decimal("0"), "orderCount": 0})
    previous = []
    for offset in range(1, 8):
        previous.append(daily_orders.get((target - timedelta(days=offset)).isoformat(), {"gmv": Decimal("0"), "refundAmount": Decimal("0"), "orderCount": 0}))

    gmv = decimal(today["gmv"])
    refund_amount = decimal(today["refundAmount"])
    order_count = int(today["orderCount"])
    refund_rate = pct(refund_amount, gmv)
    avg_order_amount = pct(gmv, Decimal(order_count)) if order_count else Decimal("0")

    yesterday_gmv = decimal(yesterday["gmv"])
    yesterday_count = Decimal(int(yesterday["orderCount"]))
    gmv_growth = pct(gmv - yesterday_gmv, yesterday_gmv) if yesterday_gmv else Decimal("0")
    order_growth = pct(Decimal(order_count) - yesterday_count, yesterday_count) if yesterday_count else Decimal("0")

    avg_gmv = sum(decimal(item["gmv"]) for item in previous) / Decimal(len(previous))
    avg_refund_rate = sum(
        pct(decimal(item["refundAmount"]), decimal(item["gmv"])) if decimal(item["gmv"]) else Decimal("0") for item in previous
    ) / Decimal(len(previous))

    return {
        "tenantId": tenant_id,
        "shopId": shop_id,
        "startDate": target.isoformat(),
        "endDate": target.isoformat(),
        "summary": {
            "gmv": fmt_number(gmv),
            "orderCount": order_count,
            "refundAmount": fmt_number(refund_amount),
            "refundRate": fmt_ratio(refund_rate),
            "avgOrderAmount": fmt_number(avg_order_amount),
            "compareYesterday": {
                "gmvGrowth": fmt_ratio(gmv_growth),
                "orderGrowth": fmt_ratio(order_growth),
            },
            "compareSevenDayAvg": {
                "gmvGrowth": fmt_ratio(pct(gmv - avg_gmv, avg_gmv) if avg_gmv else Decimal("0")),
                "refundRateDelta": fmt_ratio(refund_rate - avg_refund_rate),
            },
        },
    }


def build_negative_comment_record(
    data_dir: Path,
    target_date: str,
    tenant_id: int,
    shop_id: int,
    min_star: int,
    order_primary_item: dict[str, dict[str, object]],
) -> tuple[dict[str, object], dict[str, dict[str, object]]]:
    path = data_dir / "olist_order_reviews_dataset.csv"
    category_stats: Counter[str] = Counter()
    risk_comments: list[dict[str, object]] = []
    product_comment_stats: dict[str, dict[str, object]] = defaultdict(
        lambda: {"negativeCount": 0, "reviewScoreSum": 0, "commentCount": 0, "productName": "", "samples": []}
    )
    with path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            if row["review_creation_date"][:10] != target_date:
                continue
            try:
                score = int(row["review_score"])
            except ValueError:
                continue
            if score > min_star:
                continue
            message = normalize_text(row.get("review_comment_message") or row.get("review_comment_title") or "")
            primary_item = order_primary_item.get(row["order_id"])
            if not primary_item:
                continue
            product_id = primary_item["productId"]
            product_name = primary_item["productName"]
            labels = detect_risk_keywords(message)
            for label in labels:
                category_stats[label] += 1
            risk_comments.append(
                {
                    "commentId": row["review_id"],
                    "productId": product_id,
                    "productName": product_name,
                    "star": score,
                    "content": message or f"Olist review score={score}",
                    "riskKeywords": labels,
                }
            )
            stats = product_comment_stats[product_id]
            stats["negativeCount"] += 1
            stats["reviewScoreSum"] += score
            stats["commentCount"] += 1
            stats["productName"] = product_name
            if message:
                stats["samples"].append(message)

    risk_comments.sort(key=lambda item: (item["star"], len(str(item["content"]))))
    record = {
        "tenantId": tenant_id,
        "shopId": shop_id,
        "startDate": target_date,
        "endDate": target_date,
        "minStar": min_star,
        "summary": {
            "negativeCount": len(risk_comments),
            "riskComments": risk_comments[:10],
            "categoryStats": dict(category_stats.most_common(8)),
        },
    }
    return record, product_comment_stats


def product_reason(negative_count: int, avg_review: Decimal, sales_quantity: int) -> str:
    if negative_count >= 3 and avg_review <= Decimal("2.5"):
        return "差评集中且评分偏低，建议优先优化商品描述、质量说明与售后承诺"
    if negative_count >= 3:
        return "低星评价出现聚集，建议复核详情页卖点和履约体验"
    if sales_quantity >= 3 and negative_count > 0:
        return "销量有基础且出现负向评价，适合优先做详情页与客服话术优化"
    return "出现低星评价，建议补充规格说明、物流预期与售后说明"


def build_product_candidate_record(
    target_date: str,
    tenant_id: int,
    shop_id: int,
    product_sales: dict[str, dict[str, Decimal | int]],
    product_comment_stats: dict[str, dict[str, object]],
) -> dict[str, object]:
    candidates: list[dict[str, object]] = []
    product_ids = set(product_sales) | set(product_comment_stats)
    for product_id in product_ids:
        sales_stats = product_sales.get(product_id, {})
        comment_stats = product_comment_stats.get(product_id, {})
        sales_quantity = int(sales_stats.get("salesQuantity", 0))
        revenue = decimal(sales_stats.get("revenue", Decimal("0")))
        negative_count = int(comment_stats.get("negativeCount", 0))
        comment_count = int(comment_stats.get("commentCount", 0))
        if negative_count <= 0:
            continue
        avg_review = decimal(comment_stats.get("reviewScoreSum", 0)) / Decimal(comment_count or 1)
        score = Decimal("55")
        score += min(Decimal("24"), Decimal(negative_count * 8))
        score += min(Decimal("12"), Decimal(sales_quantity) * Decimal("1.5"))
        if avg_review <= Decimal("2"):
            score += Decimal("9")
        elif avg_review <= Decimal("2.5"):
            score += Decimal("6")
        if revenue >= Decimal("500"):
            score += Decimal("4")
        score = min(score, Decimal("99.5"))
        product_name = str(comment_stats.get("productName") or f"unknown / {product_id[:8]}")
        candidates.append(
            {
                "productId": product_id,
                "productName": product_name,
                "reason": product_reason(negative_count, avg_review, sales_quantity),
                "score": fmt_number(score, "0.1"),
                "stock": 0,
                "salesQuantity": sales_quantity,
                "negativeCount": negative_count,
            }
        )

    candidates.sort(key=lambda item: (-item["score"], -item["negativeCount"], -item["salesQuantity"], item["productId"]))
    top = candidates[:10]
    return {
        "tenantId": tenant_id,
        "shopId": shop_id,
        "startDate": target_date,
        "endDate": target_date,
        "summary": {
            "candidateCount": len(top),
            "products": top,
        },
    }


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_summary(
    path: Path,
    target_date: str,
    order_record: dict[str, object],
    comment_record: dict[str, object],
    product_record: dict[str, object],
) -> None:
    summary = order_record["summary"]
    comment_summary = comment_record["summary"]
    product_summary = product_record["summary"]
    lines = [
        "# Olist Demo Dataset Summary",
        "",
        f"- Business date: {target_date}",
        f"- GMV: {summary['gmv']}",
        f"- Order count: {summary['orderCount']}",
        f"- Refund proxy amount: {summary['refundAmount']}",
        f"- Refund proxy rate: {summary['refundRate']}",
        f"- Negative comment count: {comment_summary['negativeCount']}",
        f"- Product candidate count: {product_summary['candidateCount']}",
        "",
        "## Files",
        "",
        "- `order-summary-olist.json`",
        "- `negative-comments-olist.json`",
        "- `product-candidates-olist.json`",
    ]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()
    data_dir = Path(args.data_dir)
    output_dir = Path(args.output_dir)
    target = date.fromisoformat(args.date)

    translations = load_translation_map(data_dir)
    product_names = load_product_map(data_dir, translations)
    payments = load_payments(data_dir)
    orders = load_orders(data_dir, payments)
    daily_orders = aggregate_daily_orders(orders)
    order_primary_item, product_sales = build_order_item_views(data_dir, orders, product_names, target.isoformat())

    order_record = build_order_summary_record(target, daily_orders, args.tenant_id, args.shop_id)
    comment_record, product_comment_stats = build_negative_comment_record(
        data_dir, target.isoformat(), args.tenant_id, args.shop_id, args.min_star, order_primary_item
    )
    product_record = build_product_candidate_record(
        target.isoformat(), args.tenant_id, args.shop_id, product_sales, product_comment_stats
    )

    write_json(output_dir / "order-summary-olist.json", [order_record])
    write_json(output_dir / "negative-comments-olist.json", [comment_record])
    write_json(output_dir / "product-candidates-olist.json", [product_record])
    write_summary(output_dir / "README.md", target.isoformat(), order_record, comment_record, product_record)

    print("Generated Olist demo files:")
    print(f"  {output_dir / 'order-summary-olist.json'}")
    print(f"  {output_dir / 'negative-comments-olist.json'}")
    print(f"  {output_dir / 'product-candidates-olist.json'}")
    print(f"  {output_dir / 'README.md'}")


if __name__ == "__main__":
    main()
