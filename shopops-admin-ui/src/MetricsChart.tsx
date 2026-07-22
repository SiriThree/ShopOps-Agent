import { useEffect, useRef } from "react";
import * as echarts from "echarts";

type MetricsChartProps = {
  metrics: {
    gmv?: unknown;
    refundRate?: unknown;
    negativeCount?: unknown;
    candidateCount?: unknown;
    roi?: unknown;
  };
};

export function MetricsChart({ metrics }: MetricsChartProps) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return;
    }
    const chart = echarts.init(ref.current);
    const option: echarts.EChartsOption = {
      tooltip: { trigger: "axis" },
      grid: { left: 36, right: 16, top: 28, bottom: 28 },
      xAxis: {
        type: "category",
        data: ["GMV", "退款率", "风险评价", "商品候选", "广告ROI"],
        axisTick: { show: false }
      },
      yAxis: { type: "value", splitLine: { lineStyle: { color: "#edf1f7" } } },
      series: [
        {
          type: "bar",
          data: [
            numberValue(metrics.gmv),
            percentValue(metrics.refundRate),
            numberValue(metrics.negativeCount),
            numberValue(metrics.candidateCount),
            numberValue(metrics.roi)
          ],
          itemStyle: {
            color: (params) => ["#1677ff", "#faad14", "#f5222d", "#13c2c2", "#52c41a"][params.dataIndex]
          },
          barMaxWidth: 34
        }
      ]
    };
    chart.setOption(option);
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [metrics]);

  return <div className="metrics-chart" ref={ref} />;
}

function numberValue(value: unknown) {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : 0;
}

function percentValue(value: unknown) {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? Number((numeric * 100).toFixed(2)) : 0;
}
