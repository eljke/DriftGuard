import { BarChart3, Loader2 } from "lucide-react";
import { MetricCard, Panel } from "../../components/ui";
import { useI18n } from "../../i18n";
import type { DetectionBenchmarkReport } from "../../types";
import { boolCount, formatPercent, legacyPrecision, legacyRecall } from "../../lib/format";

export function BenchmarkPanel({
  benchmark,
  profileBenchmark,
  loading,
  profileLoading,
  onRun,
  onCompareProfiles
}: {
  benchmark?: DetectionBenchmarkReport;
  profileBenchmark: DetectionBenchmarkReport[];
  loading: boolean;
  profileLoading: boolean;
  onRun: () => void;
  onCompareProfiles: () => void;
}) {
  const { t } = useI18n();

  return (
    <Panel title={t("benchmark.title")}>
      <div className="actions">
        <button className="secondary-button" disabled={loading} onClick={onRun} type="button">
          {loading ? <Loader2 className="spin" size={16} /> : <BarChart3 size={16} />}
          {t("benchmark.run")}
        </button>
        <button className="secondary-button" disabled={profileLoading} onClick={onCompareProfiles} type="button">
          {profileLoading ? <Loader2 className="spin" size={16} /> : <BarChart3 size={16} />}
          {t("benchmark.compare")}
        </button>
      </div>
      {!benchmark ? (
        <div className="empty-state compact">
          {t("benchmark.empty")}
        </div>
      ) : (
        <div className="benchmark-stack">
          <div className="summary-grid">
            <MetricCard title={t("benchmark.profile")} value={benchmark.label} helper={t("benchmark.profileHelper")} />
            <MetricCard
              title={t("benchmark.detected")}
              value={`${benchmark.summary.detectedScenarios}/${benchmark.summary.scenarios}`}
              helper={t("benchmark.detectedHelper")}
            />
            <MetricCard title={t("benchmark.precision")} value={formatPercent(benchmark.summary.precision)} helper={`${benchmark.summary.falsePositiveEvents} false positive events`} />
            <MetricCard title={t("benchmark.recall")} value={formatPercent(benchmark.summary.recall)} helper={`${benchmark.summary.missedDriftIntervals} missed intervals`} />
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{t("benchmark.scenario")}</th>
                  <th>{t("benchmark.detected")}</th>
                  <th>{t("benchmark.events")}</th>
                  <th>{t("benchmark.falsePositive")}</th>
                  <th>{t("benchmark.missed")}</th>
                  <th>{t("benchmark.precision")}</th>
                  <th>{t("benchmark.recall")}</th>
                  <th>{t("benchmark.delay")}</th>
                </tr>
              </thead>
              <tbody>
                {benchmark.results.map((result) => (
                  <tr key={result.scenario}>
                    <td>{result.scenario}</td>
                    <td>{result.metrics.detected ? "yes" : "no"}</td>
                    <td>{result.metrics.events}</td>
                    <td>{result.metrics.falsePositiveEvents ?? boolCount(result.metrics.falsePositive)}</td>
                    <td>{result.metrics.missedDriftIntervals ?? boolCount(result.metrics.missed)}</td>
                    <td>{formatPercent(result.metrics.precision ?? legacyPrecision(result.metrics))}</td>
                    <td>{formatPercent(result.metrics.recall ?? legacyRecall(result.metrics))}</td>
                    <td>{result.metrics.firstDetectionDelay ?? result.metrics.detectionDelay ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
      {profileBenchmark.length > 0 && (
        <div className="profile-benchmark">
          <h3>{t("benchmark.profileComparison")}</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{t("benchmark.profile")}</th>
                  <th>{t("benchmark.detected")}</th>
                  <th>{t("benchmark.events")}</th>
                  <th>{t("benchmark.falsePositive")}</th>
                  <th>{t("benchmark.missed")}</th>
                  <th>{t("benchmark.precision")}</th>
                  <th>{t("benchmark.recall")}</th>
                  <th>{t("benchmark.meanDelay")}</th>
                </tr>
              </thead>
              <tbody>
                {profileBenchmark.map((report) => (
                  <tr key={report.label}>
                    <td><span className="badge">{report.label}</span></td>
                    <td>{report.summary.detectedScenarios}/{report.summary.scenarios}</td>
                    <td>{report.summary.events}</td>
                    <td>{report.summary.falsePositiveEvents}</td>
                    <td>{report.summary.missedDriftIntervals}</td>
                    <td>{formatPercent(report.summary.precision)}</td>
                    <td>{formatPercent(report.summary.recall)}</td>
                    <td>{report.summary.meanFirstDetectionDelay}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="help-text">
            {t("benchmark.profileHelp")}
          </p>
        </div>
      )}
    </Panel>
  );
}
