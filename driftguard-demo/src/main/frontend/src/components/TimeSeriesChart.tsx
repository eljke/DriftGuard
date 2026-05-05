import type { EChartsOption } from "echarts";
import * as echarts from "echarts/core";
import { DataZoomComponent, GridComponent, MarkLineComponent, TooltipComponent } from "echarts/components";
import { LineChart } from "echarts/charts";
import { CanvasRenderer } from "echarts/renderers";
import { useEffect, useMemo, useRef } from "react";
import type { DriftEvent, MetricPoint } from "../types";

echarts.use([CanvasRenderer, DataZoomComponent, GridComponent, LineChart, MarkLineComponent, TooltipComponent]);

interface TimeSeriesChartProps {
  points: MetricPoint[];
  events: DriftEvent[];
  height?: number;
}

const severityColor: Record<string, string> = {
  INFO: "#2563eb",
  WARNING: "#d97706",
  CRITICAL: "#dc2626"
};

export function TimeSeriesChart({ points, events, height = 260 }: TimeSeriesChartProps) {
  const elementRef = useRef<HTMLDivElement | null>(null);
  const option = useMemo(() => buildOption(points, events), [points, events]);

  useEffect(() => {
    if (!elementRef.current) {
      return;
    }
    const chart = echarts.init(elementRef.current, undefined, { renderer: "canvas" });
    chart.setOption(option);
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [option]);

  return <div className="chart" ref={elementRef} style={{ height }} />;
}

function buildOption(points: MetricPoint[], events: DriftEvent[]): EChartsOption {
  const sortedPoints = [...points].sort((left, right) => Date.parse(left.timestamp) - Date.parse(right.timestamp));
  const values = sortedPoints.map((point) => [point.timestamp, point.value]);
  const markLines = events.map((event) => ({
    xAxis: event.detectedAt,
    lineStyle: {
      color: severityColor[event.severity] ?? "#dc2626",
      width: event.severity === "CRITICAL" ? 2 : 1,
      type: event.severity === "CRITICAL" ? "solid" as const : "dashed" as const
    },
    label: {
      formatter: `${event.phase} · ${event.severity}`,
      color: severityColor[event.severity] ?? "#dc2626"
    }
  }));

  return {
    animation: false,
    color: ["#2563eb"],
    grid: {
      top: 24,
      right: 18,
      bottom: 58,
      left: 54
    },
    tooltip: {
      trigger: "axis",
      valueFormatter: (value) => Number(value).toFixed(3)
    },
    dataZoom: [
      { type: "inside", throttle: 40 },
      { type: "slider", height: 22, bottom: 18 }
    ],
    xAxis: {
      type: "time",
      axisLabel: {
        hideOverlap: true,
        formatter: (value: number) => {
          return new Intl.DateTimeFormat("ru-RU", {
            timeZone: "Europe/Moscow",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
          }).format(value);
        }
      }
    },
    yAxis: {
      type: "value",
      scale: true,
      splitLine: {
        lineStyle: {
          color: "#e5e7eb"
        }
      }
    },
    series: [
      {
        type: "line",
        name: "value",
        data: values,
        showSymbol: false,
        smooth: true,
        lineStyle: {
          width: 2
        },
        areaStyle: {
          color: "rgba(37, 99, 235, 0.08)"
        },
        markLine: {
          silent: true,
          symbol: "none",
          data: markLines
        }
      }
    ]
  };
}
