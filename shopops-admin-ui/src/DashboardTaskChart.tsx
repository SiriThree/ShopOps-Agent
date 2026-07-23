import React, { useEffect, useRef } from "react";
import * as echarts from "echarts";
import type { AgentTaskMetrics } from "./types";

type Props = {
  metrics?: AgentTaskMetrics;
};

export function DashboardTaskChart({ metrics }: Props) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return undefined;
    }
    const chart = echarts.init(ref.current);
    const values = [
      ["成功", Number(metrics?.success || 0), "#31795a"],
      ["失败", Number(metrics?.failed || 0), "#d14343"],
      ["运行中", Number(metrics?.running || 0), "#2f6fbb"],
      ["队列中", Number(metrics?.queued || 0), "#d9842b"],
      ["降级", Number(metrics?.degraded || 0), "#8f55d6"]
    ];
    chart.setOption({
      grid: { left: 42, right: 16, top: 24, bottom: 34 },
      tooltip: { trigger: "axis" },
      xAxis: { type: "category", data: values.map(([name]) => name) },
      yAxis: { type: "value", minInterval: 1 },
      series: [
        {
          type: "bar",
          barMaxWidth: 36,
          data: values.map(([, value, color]) => ({ value, itemStyle: { color } }))
        }
      ]
    });
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [metrics]);

  return <div className="dashboard-task-chart" ref={ref} />;
}
