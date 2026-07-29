#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import date, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CRITEO_FILE = ROOT / "data" / "criteo_attribution_dataset" / "criteo_attribution_dataset.tsv" / "pcb_dataset_final.tsv"
DEFAULT_STORE_FILE = ROOT / "data" / "Store Sales" / "holidays_events.csv"
DEFAULT_OUTPUT_DIR = ROOT / "docs" / "demo-data"


@dataclass
class CampaignStats:
    impressions: int = 0
    clicks: int = 0
    conversions: int = 0
    attributed_conversions: int = 0
    cost: float = 0.0
    cpo_value: float = 0.0


@dataclass
class ChannelStats:
    impressions: int = 0
    clicks: int = 0
    conversions: int = 0
    users: set[str] = field(default_factory=set)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate ShopOps connector JSON files from public real datasets.")
    parser.add_argument("--criteo-file", default=str(DEFAULT_CRITEO_FILE))
    parser.add_argument("--store-holidays-file", default=str(DEFAULT_STORE_FILE))
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--date", default=None, help="Target business date in YYYY-MM-DD format. Overrides the date range.")
    parser.add_argument("--start-date", default="2018-08-01")
    parser.add_argument("--end-date", default="2018-08-07")
    parser.add_argument("--tenant-id", type=int, default=1)
    parser.add_argument("--shop-id", type=int, default=1)
    parser.add_argument("--max-campaigns", type=int, default=5)
    return parser.parse_args()


def business_dates(args: argparse.Namespace) -> list[date]:
    if args.date:
        return [date.fromisoformat(args.date)]
    start = date.fromisoformat(args.start_date)
    end = date.fromisoformat(args.end_date)
    if start > end:
        raise ValueError("--start-date must be less than or equal to --end-date")
    days = []
    current = start
    while current <= end:
        days.append(current)
        current += timedelta(days=1)
    return days


def safe_float(value: str | None) -> float:
    if not value:
        return 0.0
    return float(value)


def ratio(numerator: float | int, denominator: float | int) -> float:
    if not denominator:
        return 0.0
    return round(float(numerator) / float(denominator), 4)


def money(value: float) -> float:
    return round(float(value), 6)


def build_criteo_metrics(criteo_file: Path, dates: list[date], max_campaigns: int) -> list[tuple[dict[str, object], dict[str, object]]]:
    bucket_count = len(dates)
    campaigns: list[dict[str, CampaignStats]] = [defaultdict(CampaignStats) for _ in dates]
    channels: list[dict[str, ChannelStats]] = [defaultdict(ChannelStats) for _ in dates]
    users: list[set[str]] = [set() for _ in dates]
    converting_users: list[set[str]] = [set() for _ in dates]
    repeat_touch_users: list[Counter[str]] = [Counter() for _ in dates]
    totals: list[CampaignStats] = [CampaignStats() for _ in dates]
    window_seconds = 30 * 24 * 60 * 60 / bucket_count

    with criteo_file.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        for row in reader:
            bucket_index = min(int(safe_float(row["timestamp"]) / window_seconds), bucket_count - 1)
            uid = row["uid"]
            campaign_id = row["campaign"]
            channel_id = row["cat1"]
            clicked = row["click"] == "1"
            converted = row["conversion"] == "1"
            attributed = row["attribution"] == "1"
            cost = safe_float(row["cost"])
            cpo = safe_float(row["cpo"])

            users[bucket_index].add(uid)
            repeat_touch_users[bucket_index][uid] += 1
            if converted:
                converting_users[bucket_index].add(uid)

            for stats in (totals[bucket_index], campaigns[bucket_index][campaign_id]):
                stats.impressions += 1
                stats.cost += cost
                if clicked:
                    stats.clicks += 1
                if converted:
                    stats.conversions += 1
                if attributed:
                    stats.attributed_conversions += 1
                    stats.cpo_value += cpo

            channel = channels[bucket_index][channel_id]
            channel.impressions += 1
            channel.users.add(uid)
            if clicked:
                channel.clicks += 1
            if converted:
                channel.conversions += 1

    results = []
    for index in range(bucket_count):
        results.append(finalize_criteo_bucket(
            totals[index],
            campaigns[index],
            channels[index],
            users[index],
            converting_users[index],
            repeat_touch_users[index],
            max_campaigns,
        ))
    return results


def finalize_criteo_bucket(
    totals: CampaignStats,
    campaigns: dict[str, CampaignStats],
    channels: dict[str, ChannelStats],
    users: set[str],
    converting_users: set[str],
    repeat_touch_users: Counter[str],
    max_campaigns: int,
) -> tuple[dict[str, object], dict[str, object]]:
    top_campaigns = sorted(
        campaigns.items(),
        key=lambda item: (item[1].cost, item[1].conversions, item[1].clicks),
        reverse=True,
    )[:max_campaigns]
    campaign_rows = []
    for campaign_id, stats in top_campaigns:
        campaign_rows.append({
            "campaignName": f"Criteo campaign {campaign_id}",
            "campaignId": campaign_id,
            "spend": money(stats.cost),
            "impressions": stats.impressions,
            "clicks": stats.clicks,
            "conversions": stats.conversions,
            "ctr": ratio(stats.clicks, stats.impressions),
            "conversionRate": ratio(stats.conversions, stats.clicks),
            "roi": ratio(stats.cpo_value, stats.cost),
        })

    top_channels = sorted(
        channels.items(),
        key=lambda item: (len(item[1].users), item[1].conversions, item[1].clicks),
        reverse=True,
    )[:3]
    channel_rows = []
    for channel_id, stats in top_channels:
        channel_rows.append({
            "channelName": f"Criteo traffic segment {channel_id}",
            "visitorCount": len(stats.users),
            "impressions": stats.impressions,
            "clicks": stats.clicks,
            "conversionRate": ratio(stats.conversions, len(stats.users)),
        })

    repeated_users = sum(1 for count in repeat_touch_users.values() if count > 1)
    ad_summary = {
        "spend": money(totals.cost),
        "impressions": totals.impressions,
        "clicks": totals.clicks,
        "ctr": ratio(totals.clicks, totals.impressions),
        "cpc": money(totals.cost / totals.clicks) if totals.clicks else 0,
        "conversionRate": ratio(totals.conversions, totals.clicks),
        "roi": ratio(totals.cpo_value, totals.cost),
        "conversions": totals.conversions,
        "attributedConversions": totals.attributed_conversions,
        "metricBasis": "Criteo public attribution dataset; cost and cpo are transformed anonymized values.",
        "campaigns": campaign_rows,
    }
    external_summary = {
        "visitorCount": len(users),
        "newVisitorCount": sum(1 for count in repeat_touch_users.values() if count == 1),
        "conversionRate": ratio(len(converting_users), len(users)),
        "repeatPurchaseRate": ratio(repeated_users, len(users)),
        "favoriteCount": totals.clicks,
        "cartAddCount": totals.conversions,
        "metricBasis": "Criteo public traffic proxy: clicks map to engagement, conversions map to action intent.",
        "topChannels": channel_rows,
    }
    return ad_summary, external_summary


def build_store_event_context(store_file: Path) -> dict[str, object]:
    event_count = 0
    transferred_count = 0
    type_counts: Counter[str] = Counter()
    locale_counts: Counter[str] = Counter()
    first_date = ""
    last_date = ""
    examples: list[dict[str, object]] = []

    with store_file.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            event_count += 1
            event_date = row["date"]
            first_date = event_date if not first_date else min(first_date, event_date)
            last_date = event_date if not last_date else max(last_date, event_date)
            is_transferred = row.get("transferred", "").lower() == "true"
            if is_transferred:
                transferred_count += 1
            type_counts[row.get("type", "unknown")] += 1
            locale_counts[row.get("locale", "unknown")] += 1
            if len(examples) < 5 and not is_transferred:
                examples.append({
                    "date": event_date,
                    "type": row.get("type"),
                    "locale": row.get("locale"),
                    "localeName": row.get("locale_name"),
                    "description": row.get("description"),
                })

    return {
        "source": "Corporacion Favorita Store Sales holidays_events.csv",
        "dateRange": {"start": first_date, "end": last_date},
        "eventCount": event_count,
        "transferredEventCount": transferred_count,
        "typeCounts": dict(type_counts.most_common()),
        "localeCounts": dict(locale_counts.most_common()),
        "sampleEvents": examples,
    }


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    dates = business_dates(args)
    criteo_results = build_criteo_metrics(Path(args.criteo_file), dates, args.max_campaigns)
    store_context = build_store_event_context(Path(args.store_holidays_file))

    ad_records = []
    external_records = []
    for current_date, (ad_summary, external_summary) in zip(dates, criteo_results):
        external_summary["externalEventContext"] = store_context
        common = {
            "tenantId": args.tenant_id,
            "shopId": args.shop_id,
            "startDate": current_date.isoformat(),
            "endDate": current_date.isoformat(),
        }
        ad_records.append({**common, "summary": ad_summary})
        external_records.append({**common, "summary": external_summary})
    write_json(output_dir / "ad-performance-real.json", ad_records)
    write_json(output_dir / "external-reports-real.json", external_records)
    print(f"Generated {output_dir / 'ad-performance-real.json'}")
    print(f"Generated {output_dir / 'external-reports-real.json'}")


if __name__ == "__main__":
    main()
