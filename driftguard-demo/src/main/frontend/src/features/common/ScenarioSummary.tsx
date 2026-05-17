import { MetricCard } from "../../components/ui";
import { useI18n } from "../../i18n";
import type { DemoRunResult } from "../../types";

export function ScenarioSummary({ result }: { result?: DemoRunResult }) {
  const { t } = useI18n();

  if (!result) {
    return <div className="empty-state">{t("summary.empty")}</div>;
  }
  return (
    <div className="summary-grid">
      <MetricCard title={t("summary.scenario")} value={result.title} helper={result.mode} />
      <MetricCard title={t("summary.processed")} value={`${result.processedPoints}/${result.metricPoints}`} helper={t("summary.metricPoints")} />
      <MetricCard title={t("summary.events")} value={result.events.length} helper={t("summary.detectedEvents")} />
      <MetricCard title={t("summary.quality")} value={result.quality.detected ? t("summary.detected") : t("summary.noDrift")} helper={t("summary.delay", { count: result.quality.detectionDelaySamples ?? 0 })} />
    </div>
  );
}
