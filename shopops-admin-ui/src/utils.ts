import type { Evidence } from "./types";

export function isTerminalStatus(status?: string) {
  const value = String(status || "").toUpperCase();
  return value === "SUCCESS" || value === "FAILED" || value === "DEGRADED";
}

export function normalizeEvidence(evidence: unknown): Evidence {
  if (!evidence) {
    return {};
  }
  if (typeof evidence === "string") {
    try {
      return JSON.parse(evidence) as Evidence;
    } catch {
      return {};
    }
  }
  return evidence as Evidence;
}

export function parseOutput(value: unknown): Record<string, unknown> {
  if (!value) {
    return {};
  }
  if (typeof value === "object") {
    return value as Record<string, unknown>;
  }
  try {
    return JSON.parse(String(value)) as Record<string, unknown>;
  } catch {
    return {};
  }
}

export function numberText(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return numeric.toLocaleString("zh-CN", { maximumFractionDigits: 2 });
}

export function moneyText(value: unknown) {
  const text = numberText(value);
  return text === "-" ? text : `￥${text}`;
}

export function percentText(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return `${(numeric * 100).toFixed(2)}%`;
}
