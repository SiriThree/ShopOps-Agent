import { useEffect, useRef } from "react";
import * as echarts from "echarts";
import type { AgentTaskMetrics } from "./types";

export function TaskMetricsChart({ metrics }: { metrics: AgentTaskMetrics | null }) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return;
    }
    const chart = echarts.init(ref.current);
    const status = metrics?.statusBreakdown || {};
    chart.setOption({
      tooltip: { trigger: "axis" },
      grid: { left: 34, right: 14, top: 24, bottom: 28 },
      xAxis: {
        type: "category",
        data: ["待执行", "运行中", "成功", "失败"],
        axisTick: { show: false }
      },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#edf1f7" } } },
      series: [
        {
          type: "bar",
          data: [
            status.PENDING || 0,
            status.RUNNING || 0,
            metrics?.success || 0,
            metrics?.failed || 0
          ],
          itemStyle: {
            color: (params: { dataIndex: number }) => ["#faad14", "#1677ff", "#52c41a", "#ff4d4f"][params.dataIndex]
          },
          barMaxWidth: 34
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

  return <div className="task-metrics-chart" ref={ref} />;
}
