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

const phaseLabel: Record<string, string> = {
  STARTED: "START",
  ONGOING: "ONGOING",
  RECOVERED: "RECOVERED"
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
  const labeledEventIds = labeledMarkers(events);
  const markLines = events.map((event) => ({
    xAxis: event.detectedAt,
    lineStyle: {
      color: severityColor[event.severity] ?? "#dc2626",
      width: event.severity === "CRITICAL" ? 2 : 1,
      type: event.severity === "CRITICAL" ? "solid" as const : "dashed" as const
    },
    label: {
      formatter: labeledEventIds.has(event.id) ? markerLabel(event) : "",
      color: severityColor[event.severity] ?? "#dc2626"
    }
  }));

  return {
    animation: false,
    color: ["#2563eb"],
    grid: {
      top: 34,
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

function labeledMarkers(events: DriftEvent[]) {
  const sortedEvents = [...events].sort((left, right) => Date.parse(left.detectedAt) - Date.parse(right.detectedAt));
  const labeled = new Set<string>();
  const firstOngoingByDetector = new Set<string>();
  let lastLabeledAt = 0;
  const minGapMillis = markerGapMillis(sortedEvents);

  for (const event of sortedEvents) {
    const timestamp = Date.parse(event.detectedAt);
    const detectorKey = `${event.key.service}|${event.key.metric}|${event.key.operation ?? ""}|${event.detector}`;
    const important = event.phase === "STARTED" || event.phase === "RECOVERED";
    const firstOngoing = event.phase === "ONGOING" && !firstOngoingByDetector.has(detectorKey);

    if (event.phase === "ONGOING") {
      firstOngoingByDetector.add(detectorKey);
    }
    if (!important && !firstOngoing) {
      continue;
    }
    if (!important && timestamp - lastLabeledAt < minGapMillis) {
      continue;
    }

    labeled.add(event.id);
    lastLabeledAt = timestamp;
  }

  return labeled;
}

function markerGapMillis(events: DriftEvent[]) {
  if (events.length < 2) {
    return 0;
  }
  const times = events.map((event) => Date.parse(event.detectedAt)).filter(Number.isFinite);
  const span = Math.max(...times) - Math.min(...times);
  return Math.max(20_000, span * 0.08);
}

function markerLabel(event: DriftEvent) {
  if (event.phase === "RECOVERED") {
    return phaseLabel[event.phase];
  }
  return `${phaseLabel[event.phase] ?? event.phase} · ${event.severity}`;
}
