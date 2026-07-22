import { useEffect, useRef } from "react";
import * as echarts from "echarts";

type ReportStatusChartProps = {
  success: number;
  failed: number;
  other: number;
};

export function ReportStatusChart({ success, failed, other }: ReportStatusChartProps) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return;
    }
    const chart = echarts.init(ref.current);
    chart.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0 },
      series: [
        {
          type: "pie",
          radius: ["48%", "70%"],
          center: ["50%", "44%"],
          avoidLabelOverlap: true,
          data: [
            { name: "成功", value: success, itemStyle: { color: "#52c41a" } },
            { name: "失败", value: failed, itemStyle: { color: "#ff4d4f" } },
            { name: "其他", value: other, itemStyle: { color: "#1677ff" } }
          ]
        }
      ]
    });
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [success, failed, other]);

  return <div className="report-status-chart" ref={ref} />;
}
