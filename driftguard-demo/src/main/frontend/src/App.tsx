import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  BarChart3,
  Boxes,
  Cable,
  Gauge,
  Play,
  Settings,
  Square,
  Wrench
} from "lucide-react";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { api } from "./api/client";
import { TimeSeriesChart } from "./components/TimeSeriesChart";
import type {
  DemoConfigurationView,
  DemoRunResult,
  DemoScenarioDescriptor,
  DriftEvent,
  KafkaDemoStatus,
  MetricKey,
  MetricPoint,
  ToolLink
} from "./types";

type Page = "overview" | "synthetic" | "kafka" | "configuration" | "tools";

const navigation: Array<{ page: Page; label: string; icon: typeof Activity }> = [
  { page: "overview", label: "Overview", icon: Activity },
  { page: "synthetic", label: "Synthetic", icon: BarChart3 },
  { page: "kafka", label: "Kafka Demo", icon: Cable },
  { page: "configuration", label: "Configuration", icon: Settings },
  { page: "tools", label: "Tools", icon: Wrench }
];

export default function App() {
  const [page, setPage] = useState<Page>("overview");
  const overview = useQuery({ queryKey: ["overview"], queryFn: api.overview, refetchInterval: 2500 });
  const scenarios = useQuery({ queryKey: ["scenarios"], queryFn: api.scenarios });
  const kafka = useQuery({ queryKey: ["kafka"], queryFn: api.kafkaStatus, refetchInterval: 1500 });
  const tools = useQuery({ queryKey: ["tools"], queryFn: api.tools });
  const configuration = useQuery({ queryKey: ["configuration"], queryFn: api.configuration });

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Gauge size={24} />
          </div>
          <div>
            <strong>DriftGuard</strong>
            <span>stream drift detection</span>
          </div>
        </div>
        <nav className="nav">
          {navigation.map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={page === item.page ? "nav-item active" : "nav-item"}
                key={item.page}
                onClick={() => setPage(item.page)}
                type="button"
              >
                <Icon size={18} />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <main className="main">
        <Header overview={overview.data} kafka={kafka.data} />
        {page === "overview" && <Overview result={overview.data} kafka={kafka.data} />}
        {page === "synthetic" && <SyntheticPage result={overview.data} scenarios={scenarios.data ?? []} />}
        {page === "kafka" && <KafkaPage status={kafka.data} scenarios={scenarios.data ?? []} />}
        {page === "configuration" && <ConfigurationPage configuration={configuration.data} />}
        {page === "tools" && <ToolsPage tools={tools.data ?? []} />}
      </main>
    </div>
  );
}

function Header({ overview, kafka }: { overview?: DemoRunResult; kafka?: KafkaDemoStatus }) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Europe/Moscow · real-time demo</p>
        <h1>Detection cockpit</h1>
      </div>
      <div className="status-strip">
        <StatusPill label="Synthetic" active={Boolean(overview?.running)} />
        <StatusPill label="Kafka" active={Boolean(kafka?.running)} />
      </div>
    </header>
  );
}

function Overview({ result, kafka }: { result?: DemoRunResult; kafka?: KafkaDemoStatus }) {
  const critical = countSeverity([...(result?.events ?? []), ...(kafka?.consumedEvents ?? [])], "CRITICAL");
  return (
    <section className="page-grid">
      <MetricCard title="Synthetic events" value={result?.events.length ?? 0} helper={result?.title ?? "Нет данных"} />
      <MetricCard title="Kafka events" value={kafka?.consumedEvents.length ?? 0} helper={kafka?.scenario ?? "Нет данных"} />
      <MetricCard title="Critical events" value={critical} helper="По всем demo-источникам" tone="danger" />
      <MetricCard
        title="Kafka progress"
        value={`${kafka?.producedPoints ?? 0}/${kafka?.totalPoints ?? 0}`}
        helper={kafka?.inputTopic ?? "Topic не загружен"}
      />
      <Panel className="wide" title="Последний synthetic запуск">
        <ScenarioSummary result={result} />
      </Panel>
      <Panel className="wide" title="Kafka streams">
        <StreamGrid points={kafka?.samplePoints ?? []} events={kafka?.consumedEvents ?? []} />
      </Panel>
    </section>
  );
}

function SyntheticPage({ result, scenarios }: { result?: DemoRunResult; scenarios: DemoScenarioDescriptor[] }) {
  const queryClient = useQueryClient();
  const run = useMutation({
    mutationFn: api.runScenario,
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });
  const live = useMutation({
    mutationFn: api.startLive,
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });
  const stopLive = useMutation({
    mutationFn: api.stopLive,
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });

  return (
    <section className="stack">
      <Panel title="Synthetic scenarios">
        <ScenarioButtons scenarios={scenarios} onRun={(id) => run.mutate(id)} onLive={(id) => live.mutate(id)} />
      </Panel>
      <Panel title="Result">
        <ScenarioSummary result={result} />
        <div className="actions">
          <button className="secondary-button" disabled={!result?.running} onClick={() => stopLive.mutate()} type="button">
            <Square size={16} />
            Stop live
          </button>
        </div>
      </Panel>
      <Panel title="Synthetic chart">
        <StreamGrid points={result?.samplePoints ?? []} events={result?.events ?? []} />
      </Panel>
      <EventsTable events={result?.events ?? []} />
    </section>
  );
}

function KafkaPage({ status, scenarios }: { status?: KafkaDemoStatus; scenarios: DemoScenarioDescriptor[] }) {
  const queryClient = useQueryClient();
  const start = useMutation({
    mutationFn: api.startKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });
  const stop = useMutation({
    mutationFn: api.stopKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });
  const kafkaScenarios = scenarios.filter((scenario) => scenario.id !== "seasonal-latency");

  return (
    <section className="stack">
      <Panel title="Kafka scenarios">
        <ScenarioButtons scenarios={kafkaScenarios} onRun={(id) => start.mutate(id)} />
        <div className="actions">
          <button className="secondary-button" disabled={!status?.running} onClick={() => stop.mutate()} type="button">
            <Square size={16} />
            Stop Kafka demo
          </button>
        </div>
      </Panel>
      <div className="producer-grid">
        {(status?.producers ?? []).map((producer) => (
          <article className="producer-card" key={producer.id}>
            <div>
              <strong>{producer.service}</strong>
              <span>{producer.metric} · {producer.operation ?? "-"}</span>
            </div>
            <Progress value={producer.producedPoints} max={producer.totalPoints} />
          </article>
        ))}
      </div>
      <Panel title="Kafka metric streams">
        <StreamGrid points={status?.samplePoints ?? []} events={status?.consumedEvents ?? []} />
      </Panel>
      <EventsTable events={status?.consumedEvents ?? []} />
    </section>
  );
}

function ConfigurationPage({ configuration }: { configuration?: DemoConfigurationView }) {
  if (!configuration) {
    return <Panel title="Configuration">Загрузка конфигурации...</Panel>;
  }
  return (
    <section className="stack">
      <div className="page-grid">
        <MetricCard title="Aggressiveness" value={configuration.aggressiveness.level} helper={configuration.aggressiveness.description} />
        <MetricCard title="Kafka input" value={configuration.kafka.inputTopic} helper={configuration.kafka.bootstrapServers} />
        <MetricCard title="Kafka output" value={configuration.kafka.outputTopic} helper={configuration.kafka.applicationId} />
        <MetricCard title="Playback" value={configuration.kafka.playbackInterval} helper="Интервал публикации точек" />
      </div>
      <Panel title="Detector definitions">
        <div className="detector-grid">
          {configuration.detectors.map((detector) => (
            <article className="detector-card" key={detector.name}>
              <div className="detector-head">
                <strong>{detector.name}</strong>
                <span className="badge">{detector.sensitivity}</span>
              </div>
              <dl>
                <dt>Algorithm</dt>
                <dd>{detector.algorithm}</dd>
                <dt>Metrics</dt>
                <dd>{detector.metrics.join(", ") || "any"}</dd>
                <dt>Warning</dt>
                <dd>{detector.warningThreshold} / p={detector.warningPValue}</dd>
                <dt>Critical</dt>
                <dd>{detector.criticalThreshold} / p={detector.criticalPValue}</dd>
                <dt>Emission</dt>
                <dd>{detector.emissionPolicy.minConsecutiveSignals} signals, {detector.emissionPolicy.cooldown}</dd>
              </dl>
            </article>
          ))}
        </div>
      </Panel>
    </section>
  );
}

function ToolsPage({ tools }: { tools: ToolLink[] }) {
  return (
    <section className="tool-grid">
      {tools.map((tool) => (
        <a className="tool-card" href={tool.url} key={tool.id} rel="noreferrer" target={tool.url.startsWith("http") ? "_blank" : undefined}>
          <Boxes size={22} />
          <strong>{tool.title}</strong>
          <span>{tool.description}</span>
        </a>
      ))}
    </section>
  );
}

function ScenarioButtons({
  scenarios,
  onRun,
  onLive
}: {
  scenarios: DemoScenarioDescriptor[];
  onRun: (scenario: string) => void;
  onLive?: (scenario: string) => void;
}) {
  return (
    <div className="scenario-grid">
      {scenarios.map((scenario) => (
        <article className="scenario-card" key={scenario.id}>
          <div>
            <strong>{scenario.title}</strong>
            <span>{scenario.description}</span>
          </div>
          <div className="scenario-actions">
            <button className="primary-button" onClick={() => onRun(scenario.id)} type="button">
              <Play size={16} />
              Run
            </button>
            {onLive && (
              <button className="secondary-button" onClick={() => onLive(scenario.id)} type="button">
                Live
              </button>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}

function StreamGrid({ points, events }: { points: MetricPoint[]; events: DriftEvent[] }) {
  const groups = useMemo(() => groupStreams(points), [points]);
  if (groups.length === 0) {
    return <div className="empty-state">Метрики пока не опубликованы.</div>;
  }
  return (
    <div className="stream-grid">
      {groups.map((group) => {
        const streamEvents = events.filter((event) => streamId(event.key) === group.id);
        return (
          <article className="stream-card" key={group.id}>
            <div className="stream-head">
              <div>
                <strong>{group.service}</strong>
                <span>{group.metric} · {group.operation || "-"}</span>
              </div>
              <span className="badge">{group.points.length} points · {streamEvents.length} events</span>
            </div>
            <TimeSeriesChart points={group.points} events={streamEvents} />
          </article>
        );
      })}
    </div>
  );
}

function EventsTable({ events }: { events: DriftEvent[] }) {
  return (
    <Panel title="Drift events">
      {events.length === 0 ? (
        <div className="empty-state">Событий drift-а пока нет.</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Time MSK</th>
                <th>Severity</th>
                <th>Service</th>
                <th>Metric</th>
                <th>Algorithm</th>
                <th>Score</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {events.slice().reverse().map((event) => (
                <tr key={event.id}>
                  <td>{formatMoscow(event.detectedAt)}</td>
                  <td><SeverityBadge severity={event.severity} /></td>
                  <td>{event.key.service}</td>
                  <td>{event.key.metric}</td>
                  <td>{event.algorithm}</td>
                  <td>{event.score.toFixed(3)}</td>
                  <td>{event.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}

function ScenarioSummary({ result }: { result?: DemoRunResult }) {
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

function MetricCard({ title, value, helper, tone }: { title: string; value: string | number; helper: string; tone?: "danger" }) {
  return (
    <article className={tone === "danger" ? "metric-card danger" : "metric-card"}>
      <span>{title}</span>
      <strong>{value}</strong>
      <p>{helper}</p>
    </article>
  );
}

function Panel({ title, children, className = "" }: { title: string; children: ReactNode; className?: string }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-title">
        <h2>{title}</h2>
      </div>
      {children}
    </section>
  );
}

function StatusPill({ label, active }: { label: string; active: boolean }) {
  return <span className={active ? "status-pill active" : "status-pill"}>{label}: {active ? "running" : "idle"}</span>;
}

function SeverityBadge({ severity }: { severity: string }) {
  return <span className={`severity ${severity.toLowerCase()}`}>{severity}</span>;
}

function Progress({ value, max }: { value: number; max: number }) {
  const percent = max === 0 ? 0 : Math.min(100, Math.round((value / max) * 100));
  return (
    <div>
      <div className="progress-label">
        <span>{value}/{max}</span>
        <span>{percent}%</span>
      </div>
      <div className="progress">
        <span style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

function groupStreams(points: MetricPoint[]) {
  const byStream = new Map<string, { id: string; service: string; metric: string; operation?: string; points: MetricPoint[] }>();
  for (const point of points) {
    const id = streamId(point.key);
    if (!byStream.has(id)) {
      byStream.set(id, {
        id,
        service: point.key.service,
        metric: point.key.metric,
        operation: point.key.operation,
        points: []
      });
    }
    byStream.get(id)!.points.push(point);
  }
  return [...byStream.values()].sort((left, right) => left.id.localeCompare(right.id));
}

function streamId(key: MetricKey) {
  return `${key.service}|${key.metric}|${key.operation ?? ""}`;
}

function countSeverity(events: DriftEvent[], severity: string) {
  return events.filter((event) => event.severity === severity).length;
}

function formatMoscow(value: string) {
  return new Intl.DateTimeFormat("ru-RU", {
    timeZone: "Europe/Moscow",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(new Date(value));
}
