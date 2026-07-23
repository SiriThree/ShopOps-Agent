import React, { useEffect, useRef } from "react";
import * as echarts from "echarts";
import type { ToolCallLog } from "./types";

type Props = {
  logs: ToolCallLog[];
};

const colors: Record<string, string> = {
  SUCCESS: "#31795a",
  FAILED: "#d14343",
  APPROVAL_REQUIRED: "#8f55d6",
  RUNNING: "#2f6fbb"
};

export function ToolStatusChart({ logs }: Props) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return undefined;
    }
    const chart = echarts.init(ref.current);
    const counts = logs.reduce<Record<string, number>>((acc, log) => {
      const key = log.status || "UNKNOWN";
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    const data = Object.entries(counts).map(([name, value]) => ({
      name,
      value,
      itemStyle: { color: colors[name] || "#637083" }
    }));
    chart.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0, left: "center" },
      series: [
        {
          name: "调用状态",
          type: "pie",
          radius: ["42%", "68%"],
          center: ["50%", "44%"],
          label: { formatter: "{b}: {c}" },
          data: data.length ? data : [{ name: "暂无日志", value: 1, itemStyle: { color: "#d9dee8" } }]
        }
      ]
    });
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [logs]);

  return <div className="tool-status-chart" ref={ref} />;
}
