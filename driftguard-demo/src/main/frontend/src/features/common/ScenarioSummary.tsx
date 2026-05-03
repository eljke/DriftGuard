import { MetricCard } from "../../components/ui";
import type { DemoRunResult } from "../../types";

export function ScenarioSummary({ result }: { result?: DemoRunResult }) {
  if (!result) {
    return <div className="empty-state">Результат ещё не загружен.</div>;
  }
  return (
    <div className="summary-grid">
      <MetricCard title="Scenario" value={result.title} helper={result.mode} />
      <MetricCard title="Processed" value={`${result.processedPoints}/${result.metricPoints}`} helper="Metric points" />
      <MetricCard title="Events" value={result.events.length} helper="Detected drift events" />
      <MetricCard title="Quality" value={result.quality.detected ? "Detected" : "No drift"} helper={`Delay: ${result.quality.detectionDelaySamples} samples`} />
    </div>
  );
}
