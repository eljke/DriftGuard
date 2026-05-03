import { BarChart3, Loader2 } from "lucide-react";
import { MetricCard, Panel } from "../../components/ui";
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
  return (
    <Panel title="Synthetic benchmark">
      <div className="actions">
        <button className="secondary-button" disabled={loading} onClick={onRun} type="button">
          {loading ? <Loader2 className="spin" size={16} /> : <BarChart3 size={16} />}
          Run benchmark
        </button>
        <button className="secondary-button" disabled={profileLoading} onClick={onCompareProfiles} type="button">
          {profileLoading ? <Loader2 className="spin" size={16} /> : <BarChart3 size={16} />}
          Compare profiles
        </button>
      </div>
      {!benchmark ? (
        <div className="empty-state compact">
          Benchmark прогоняет все synthetic scenarios на текущем detector profile и считает precision, recall, false positives и пропуски.
        </div>
      ) : (
        <div className="benchmark-stack">
          <div className="summary-grid">
            <MetricCard title="Profile" value={benchmark.label} helper="Runtime detector profile" />
            <MetricCard
              title="Detected"
              value={`${benchmark.summary.detectedScenarios}/${benchmark.summary.scenarios}`}
              helper="Scenarios with expected drift detected"
            />
            <MetricCard title="Precision" value={formatPercent(benchmark.summary.precision)} helper={`${benchmark.summary.falsePositiveEvents} false positive events`} />
            <MetricCard title="Recall" value={formatPercent(benchmark.summary.recall)} helper={`${benchmark.summary.missedDriftIntervals} missed intervals`} />
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Scenario</th>
                  <th>Detected</th>
                  <th>Events</th>
                  <th>False positives</th>
                  <th>Missed</th>
                  <th>Precision</th>
                  <th>Recall</th>
                  <th>Delay</th>
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
          <h3>Profile comparison</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Profile</th>
                  <th>Detected</th>
                  <th>Events</th>
                  <th>False positives</th>
                  <th>Missed</th>
                  <th>Precision</th>
                  <th>Recall</th>
                  <th>Mean delay</th>
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
            Сравнение профилей помогает увидеть компромисс: раннее обнаружение обычно увеличивает риск false positives, а консервативный профиль может повысить задержку или пропуски.
          </p>
        </div>
      )}
    </Panel>
  );
}
