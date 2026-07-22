import React, { useEffect, useRef } from "react";
import * as echarts from "echarts";

type Props = {
  breakdown?: Record<string, number>;
};

const riskLabels: Record<string, string> = {
  HIGH: "高风险",
  MEDIUM: "中风险",
  LOW: "低风险",
  UNKNOWN: "未知"
};

export function AuditRiskChart({ breakdown }: Props) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return undefined;
    }
    const chart = echarts.init(ref.current);
    const entries = Object.entries(breakdown || {}).filter(([, value]) => Number(value) > 0);
    chart.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0, left: "center" },
      color: ["#d14343", "#d9842b", "#31795a", "#637083"],
      series: [
        {
          name: "风险分布",
          type: "pie",
          radius: ["42%", "68%"],
          center: ["50%", "44%"],
          avoidLabelOverlap: true,
          label: { formatter: "{b}: {c}" },
          data: entries.length
            ? entries.map(([name, value]) => ({ name: riskLabels[name] || name, value }))
            : [{ name: "暂无事件", value: 1, itemStyle: { color: "#d9dee8" } }]
        }
      ]
    });
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [breakdown]);

  return <div className="audit-risk-chart" ref={ref} />;
}
