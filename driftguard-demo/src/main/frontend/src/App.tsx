import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  AlertCircle,
  BarChart3,
  Boxes,
  Cable,
  Gauge,
  Loader2,
  Play,
  Search,
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
  DetectionBenchmarkReport,
  DemoRunResult,
  DemoStoredDriftEvent,
  DemoScenarioDescriptor,
  DriftEvent,
  KafkaDemoStatus,
  KafkaOperationsSnapshot,
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
  const overview = useQuery({ queryKey: ["overview"], queryFn: api.overview, refetchInterval: 750 });
  const scenarios = useQuery({ queryKey: ["scenarios"], queryFn: api.scenarios });
  const kafka = useQuery({ queryKey: ["kafka"], queryFn: api.kafkaStatus, refetchInterval: 750 });
  const kafkaOperations = useQuery({ queryKey: ["kafka-operations"], queryFn: api.kafkaOperations, refetchInterval: 1500 });
  const tools = useQuery({ queryKey: ["tools"], queryFn: api.tools });
  const configuration = useQuery({ queryKey: ["configuration"], queryFn: api.configuration });
  const storedEvents = useQuery({ queryKey: ["stored-events"], queryFn: api.storedEvents, refetchInterval: 750 });

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
        {page === "overview" && <Overview result={overview.data} kafka={kafka.data} storedEvents={storedEvents.data ?? []} />}
        {page === "synthetic" && <SyntheticPage result={overview.data} scenarios={scenarios.data ?? []} />}
        {page === "kafka" && <KafkaPage status={kafka.data} operations={kafkaOperations.data} scenarios={scenarios.data ?? []} configuration={configuration.data} />}
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

function Overview({ result, kafka, storedEvents }: { result?: DemoRunResult; kafka?: KafkaDemoStatus; storedEvents: DemoStoredDriftEvent[] }) {
  const queryClient = useQueryClient();
  const critical = countSeverity([...(result?.events ?? []), ...(kafka?.consumedEvents ?? [])], "CRITICAL");
  const clearStoredEvents = useMutation({
    mutationFn: api.clearStoredEvents,
    onSuccess: () => queryClient.setQueryData(["stored-events"], [])
  });

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
        <StreamGrid points={kafka?.samplePoints ?? []} events={kafka?.consumedEvents ?? []} running={Boolean(kafka?.running)} />
      </Panel>
      <Panel className="wide" title="Recent stored drift events">
        <div className="panel-toolbar">
          <span className="help-text">Общий recent stream из synthetic, live и Kafka demo.</span>
          <button
            className="secondary-button"
            disabled={storedEvents.length === 0 || clearStoredEvents.isPending}
            onClick={() => clearStoredEvents.mutate()}
            type="button"
          >
            {clearStoredEvents.isPending ? <Loader2 className="spin" size={16} /> : null}
            Clear stored events
          </button>
        </div>
        {clearStoredEvents.error && <Notice tone="error" text={readableError(clearStoredEvents.error)} />}
        <StoredEventsTable storedEvents={storedEvents} />
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
  const benchmark = useQuery({ queryKey: ["benchmark"], queryFn: api.benchmark, enabled: false });
  const profileBenchmark = useQuery({ queryKey: ["profile-benchmark"], queryFn: api.benchmarkProfiles, enabled: false });
  const busy = run.isPending || live.isPending || stopLive.isPending;
  const error = run.error ?? live.error ?? stopLive.error;

  return (
    <section className="stack">
      <Panel title="Synthetic scenarios">
        {busy && <Notice tone="info" text="Команда выполняется. Результат обновится автоматически." />}
        {error && <Notice tone="error" text={readableError(error)} />}
        <ScenarioButtons scenarios={scenarios} busy={busy} onRun={(id) => run.mutate(id)} onLive={(id) => live.mutate(id)} />
      </Panel>
      <BenchmarkPanel
        benchmark={benchmark.data}
        profileBenchmark={profileBenchmark.data ?? []}
        loading={benchmark.isFetching}
        profileLoading={profileBenchmark.isFetching}
        onRun={() => benchmark.refetch()}
        onCompareProfiles={() => profileBenchmark.refetch()}
      />
      <Panel title="Result">
        <ScenarioSummary result={result} />
        {result?.running && <Notice tone="info" text="Live playback активен: точки и события появляются по мере обработки." />}
        <div className="actions">
          <button className="secondary-button" disabled={!result?.running || stopLive.isPending} onClick={() => stopLive.mutate()} type="button">
            <Square size={16} />
            Stop live
          </button>
        </div>
      </Panel>
      <Panel title="Synthetic chart">
        <StreamGrid points={result?.samplePoints ?? []} events={result?.events ?? []} running={Boolean(result?.running)} />
      </Panel>
      <IncidentsPanel events={result?.events ?? []} />
      <EventsTable events={result?.events ?? []} />
    </section>
  );
}

function KafkaPage({
  status,
  operations,
  scenarios,
  configuration
}: {
  status?: KafkaDemoStatus;
  operations?: KafkaOperationsSnapshot;
  scenarios: DemoScenarioDescriptor[];
  configuration?: DemoConfigurationView;
}) {
  const queryClient = useQueryClient();
  const [replaySpeed, setReplaySpeed] = useState(2);
  const [replayProfile, setReplayProfile] = useState("");
  const [resetState, setResetState] = useState(true);
  const start = useMutation({
    mutationFn: api.startKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });
  const stop = useMutation({
    mutationFn: api.stopKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });
  const replay = useMutation({
    mutationFn: api.replayKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });
  const kafkaScenarios = scenarios.filter((scenario) => scenario.id !== "seasonal-latency");
  const busy = start.isPending || replay.isPending || stop.isPending;
  const error = start.error ?? replay.error ?? stop.error;
  const profiles = configuration?.availableProfiles ?? [];

  return (
    <section className="stack kafka-page">
      <KafkaOperationsPanel status={status} operations={operations} />

      <Panel title="Run Kafka scenario" className="control-panel">
        {busy && <Notice tone="info" text="Kafka demo запускается. Создаются topic-и, topology, producer и consumer." />}
        {status?.running && <Notice tone="info" text="Kafka demo работает. График и таблица обновляются каждые 750 мс." />}
        {status?.replay && <Notice tone="info" text={`Replay mode активен: скорость ${status.speed}x, detector state был сброшен перед запуском.`} />}
        {status?.error && <Notice tone="error" text={status.error} />}
        {error && <Notice tone="error" text={readableError(error)} />}
        <ReplayControls
          disabled={busy || Boolean(status?.running)}
          profiles={profiles}
          resetState={resetState}
          selectedProfile={replayProfile}
          speed={replaySpeed}
          onProfileChange={setReplayProfile}
          onResetStateChange={setResetState}
          onSpeedChange={setReplaySpeed}
        />
        <ScenarioButtons
          scenarios={kafkaScenarios}
          busy={busy || Boolean(status?.running)}
          onRun={(id) => start.mutate(id)}
          onReplay={(id) => replay.mutate({
            scenario: id,
            speed: replaySpeed,
            resetState,
            profile: replayProfile || undefined
          })}
        />
        <div className="actions">
          <button className="secondary-button" disabled={!status?.running || stop.isPending} onClick={() => stop.mutate()} type="button">
            <Square size={16} />
            Stop Kafka demo
          </button>
        </div>
      </Panel>

      <ProducerStrip status={status} />

      <Panel title="Kafka metric streams">
        <StreamGrid points={status?.samplePoints ?? []} events={status?.consumedEvents ?? []} running={Boolean(status?.running)} />
      </Panel>
      <IncidentsPanel events={status?.consumedEvents ?? []} />
      <EventsTable events={status?.consumedEvents ?? []} />
    </section>
  );
}

function ConfigurationPage({ configuration }: { configuration?: DemoConfigurationView }) {
  const queryClient = useQueryClient();
  const updateProfile = useMutation({
    mutationFn: api.updateProfile,
    onSuccess: (data) => queryClient.setQueryData(["configuration"], data)
  });

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
      <Panel title="Registered algorithms">
        <div className="algorithm-list">
          {configuration.registeredAlgorithms.map((algorithm) => (
            <span className="badge" key={algorithm}>{algorithm}</span>
          ))}
        </div>
        <p className="panel-note">
          Starter автоматически объединяет встроенные алгоритмы DriftGuard и пользовательские DetectorAlgorithm bean-ы.
        </p>
      </Panel>
      <Panel title="Runtime detector profile">
        {updateProfile.isPending && <Notice tone="info" text="Профиль применяется: engine пересоздаётся, состояние detector-ов сбрасывается." />}
        {updateProfile.error && <Notice tone="error" text={readableError(updateProfile.error)} />}
        <div className="actions">
          {configuration.availableProfiles.map((profile) => {
            const active = configuration.aggressiveness.level.toUpperCase() === profile;
            return (
              <button
                className={active ? "primary-button" : "secondary-button"}
                disabled={updateProfile.isPending || active}
                key={profile}
                onClick={() => updateProfile.mutate(profile)}
                type="button"
              >
                {updateProfile.isPending ? <Loader2 className="spin" size={16} /> : null}
                {profile}
              </button>
            );
          })}
        </div>
        <p className="help-text">
          Профиль реально меняет runtime DriftDetectorEngine. Kafka topology не пересобирается: она вызывает runtime.detect(), поэтому следующие сообщения сразу идут через новый engine.
        </p>
      </Panel>
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
                <dd>
                  {detector.emissionPolicy.minConsecutiveSignals} signals, {detector.emissionPolicy.cooldown}, recovery {detector.emissionPolicy.recoveryConsecutiveNormal}
                </dd>
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

function ReplayControls({
  disabled,
  profiles,
  resetState,
  selectedProfile,
  speed,
  onProfileChange,
  onResetStateChange,
  onSpeedChange
}: {
  disabled: boolean;
  profiles: string[];
  resetState: boolean;
  selectedProfile: string;
  speed: number;
  onProfileChange: (profile: string) => void;
  onResetStateChange: (reset: boolean) => void;
  onSpeedChange: (speed: number) => void;
}) {
  return (
    <div className="replay-controls">
      <label className="field">
        <span>Replay speed</span>
        <select
          disabled={disabled}
          value={speed}
          onChange={(event) => onSpeedChange(Number(event.target.value))}
        >
          <option value={0.5}>0.5x</option>
          <option value={1}>1x</option>
          <option value={2}>2x</option>
          <option value={5}>5x</option>
          <option value={10}>10x</option>
        </select>
      </label>

      <label className="field">
        <span>Detector profile</span>
        <select
          disabled={disabled || profiles.length === 0}
          value={selectedProfile}
          onChange={(event) => onProfileChange(event.target.value)}
        >
          <option value="">Current profile</option>
          {profiles.map((profile) => (
            <option key={profile} value={profile}>{profile}</option>
          ))}
        </select>
      </label>

      <label className="checkbox-field">
        <input
          checked={resetState}
          disabled={disabled}
          type="checkbox"
          onChange={(event) => onResetStateChange(event.target.checked)}
        />
        <span>Reset detector state before replay</span>
      </label>

      <p className="help-text">
        Replay переигрывает тот же synthetic scenario через Kafka. Это удобно для сравнения профилей и скорости обнаружения на одинаковом потоке.
      </p>
    </div>
  );
}

function ScenarioButtons({
                           scenarios,
                           busy,
                           onRun,
                           onReplay,
                           onLive
                         }: {
  scenarios: DemoScenarioDescriptor[];
  busy?: boolean;
  onRun: (scenario: string) => void;
  onReplay?: (scenario: string) => void;
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
                <button className="primary-button" disabled={busy} onClick={() => onRun(scenario.id)} type="button">
                  {busy ? <Loader2 className="spin" size={16} /> : <Play size={16} />}
                  Run
                </button>
                {onReplay && (
                    <button className="secondary-button" disabled={busy} onClick={() => onReplay(scenario.id)} type="button">
                      Replay 2x
                    </button>
                )}
                {onLive && (
                    <button className="secondary-button" disabled={busy} onClick={() => onLive(scenario.id)} type="button">
                      Live
                    </button>
                )}
              </div>
            </article>
        ))}
      </div>
  );
}

function StreamGrid({ points, events, running = false }: { points: MetricPoint[]; events: DriftEvent[]; running?: boolean }) {
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
            {running && streamEvents.length === 0 && (
              <div className="inline-hint">Поток идёт, detector ждёт достаточное окно и подтверждение сигнала.</div>
            )}
            <TimeSeriesChart points={group.points} events={streamEvents} />
          </article>
        );
      })}
    </div>
  );
}

function EventsTable({ events }: { events: DriftEvent[] }) {
  const [severity, setSeverity] = useState("ALL");
  const [phase, setPhase] = useState("ALL");
  const [query, setQuery] = useState("");
  const visibleEvents = useMemo(
    () => events
      .filter((event) => severity === "ALL" || event.severity === severity)
      .filter((event) => phase === "ALL" || event.phase === phase)
      .filter((event) => eventMatchesQuery(event, query))
      .slice()
      .reverse(),
    [events, phase, query, severity]
  );

  return (
    <Panel title="Drift events">
      {events.length === 0 ? (
        <div className="empty-state">Событий drift-а пока нет.</div>
      ) : (
        <>
          <EventFilters
            phase={phase}
            query={query}
            severity={severity}
            total={events.length}
            visible={visibleEvents.length}
            onPhaseChange={setPhase}
            onQueryChange={setQuery}
            onSeverityChange={setSeverity}
          />
          {visibleEvents.length === 0 ? (
            <div className="empty-state compact">По текущим фильтрам событий нет.</div>
          ) : (
            <div className="table-wrap">
              <table className="events-table">
                <thead>
                  <tr>
                    <th>Time MSK</th>
                    <th>Event</th>
                    <th>Metric</th>
                    <th>Values</th>
                    <th>Explanation</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleEvents.map((event) => (
                    <tr key={event.id}>
                      <td>{formatMoscow(event.detectedAt)}</td>
                      <td>
                        <div className="event-cell">
                          <span className={`severity ${event.severity.toLowerCase()}`}>{event.severity}</span>
                          <span className={`phase ${event.phase.toLowerCase()}`}>{event.phase}</span>
                          <span className="muted-line">{event.algorithm}</span>
                        </div>
                      </td>
                      <td>
                        <div className="event-cell">
                          <strong>{event.key.service}</strong>
                          <span className="muted-line">{event.key.metric} · {event.key.operation ?? "-"}</span>
                        </div>
                      </td>
                      <td><EventValueSummary event={event} /></td>
                      <td className="event-explanation">{eventExplanation(event)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </Panel>
  );
}

function KafkaOperationsPanel({ status, operations }: { status?: KafkaDemoStatus; operations?: KafkaOperationsSnapshot }) {
  const producerCount = status?.producers.length ?? 0;
  const activeProducers = status?.producers.filter((producer) => producer.running).length ?? 0;
  const progressValue = status?.totalPoints ? Math.round(((status.producedPoints ?? 0) / status.totalPoints) * 100) : 0;
  const lastEvent = status?.consumedEvents.at(-1);
  const failedPoints = operations?.metrics.failedPoints ?? 0;
  const routedErrors = operations?.metrics.routedErrors ?? 0;

  return (
    <Panel title="Kafka operations" className="ops-panel">
      <div className={status?.running ? "ops-hero active" : "ops-hero"}>
        <div>
          <p className="eyebrow">Stateful Kafka pipeline</p>
          <h2>{status?.running ? "Pipeline is running" : "Pipeline is idle"}</h2>
          <p>{status?.scenario ?? "Scenario не выбран"} · {status?.replay ? "Replay" : "Normal"} · {status?.speed ?? 1}x</p>
        </div>
        <div className="ops-progress-ring" aria-label={`Replay progress ${progressValue}%`}>
          <strong>{progressValue}%</strong>
          <span>{status?.producedPoints ?? 0}/{status?.totalPoints ?? 0}</span>
        </div>
      </div>

      <div className="ops-kpis">
        <MetricCard title="Processed" value={formatNumber(operations?.metrics.processedPoints)} helper="MetricPoint обработано" />
        <MetricCard title="Events" value={formatNumber(operations?.metrics.emittedEvents ?? status?.consumedEvents.length)} helper="DriftEvent в pipeline" />
        <MetricCard title="Errors / DLQ" value={`${formatNumber(failedPoints)} / ${formatNumber(routedErrors)}`} helper="Ошибки detector-а" tone={failedPoints > 0 ? "danger" : undefined} />
        <MetricCard title="Latency" value={`${formatNumber(operations?.metrics.meanDurationMillis)} ms`} helper={operations?.telemetryEnabled ? "Micrometer timer" : "Telemetry недоступна"} />
      </div>

      <div className="ops-meta-row">
        <span><strong>Producers</strong>{activeProducers}/{producerCount}</span>
        <span><strong>Input</strong>{operations?.streamsInputTopics?.join(", ") || status?.inputTopic || "—"}</span>
        <span><strong>Output</strong>{operations?.outputTopic || status?.outputTopic || "—"}</span>
        <span><strong>State store</strong>{operations?.runtimeStateStoreName ?? "—"}</span>
        <span><strong>Error mode</strong>{operations?.detectionErrorMode ?? "—"}</span>
        <span><strong>Last event</strong>{lastEvent ? `${lastEvent.severity} · ${lastEvent.key.metric}` : "—"}</span>
      </div>

      <div className="ops-checklist compact">
        <OperationCheck active={Boolean(status?.enabled)} label="Kafka demo enabled" detail={status?.outputTopic ?? "output topic недоступен"} />
        <OperationCheck active={Boolean(status?.running)} label="Streams topology running" detail={status?.inputTopic ?? "input topic недоступен"} />
        <OperationCheck active={Boolean(status?.replay)} label="Replay mode" detail={`speed ${status?.speed ?? 1}x`} />
        <OperationCheck active={!status?.error} label="No runtime error" detail={status?.error ?? "Ошибок в demo status нет"} />
      </div>
    </Panel>
  );
}

function ProducerStrip({ status }: { status?: KafkaDemoStatus }) {
  const producers = status?.producers ?? [];

  if (producers.length === 0) {
    return null;
  }

  return (
    <Panel title="Kafka producers" className="quiet-panel">
      <div className="producer-strip">
        {producers.map((producer) => (
          <article className="producer-card compact" key={producer.id}>
            <div>
              <strong>{producer.service}</strong>
              <span>{producer.metric} · {producer.operation ?? "-"}</span>
            </div>
            <Progress value={producer.producedPoints} max={producer.totalPoints} />
          </article>
        ))}
      </div>
    </Panel>
  );
}

function EventValueSummary({ event }: { event: DriftEvent }) {
  return (
    <dl className="event-values">
      <div>
        <dt>Current</dt>
        <dd>{formatNumber(event.currentValue)}</dd>
      </div>
      <div>
        <dt>Baseline</dt>
        <dd>{formatNumber(event.baselineValue)}</dd>
      </div>
      <div>
        <dt>Score</dt>
        <dd>{formatNumber(event.score)}</dd>
      </div>
    </dl>
  );
}

function OperationCheck({ active, label, detail }: { active: boolean; label: string; detail: string }) {
  return (
    <div className={active ? "operation-check active" : "operation-check"}>
      <span className="operation-dot" />
      <div>
        <strong>{label}</strong>
        <span>{detail}</span>
      </div>
    </div>
  );
}

function EventFilters({
  phase,
  query,
  severity,
  total,
  visible,
  onPhaseChange,
  onQueryChange,
  onSeverityChange
}: {
  phase: string;
  query: string;
  severity: string;
  total: number;
  visible: number;
  onPhaseChange: (phase: string) => void;
  onQueryChange: (query: string) => void;
  onSeverityChange: (severity: string) => void;
}) {
  return (
    <div className="event-filters">
      <label className="search-field">
        <Search size={16} />
        <input
          placeholder="Search service, metric, detector, algorithm or reason"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
        />
      </label>
      <label className="field compact-field">
        <span>Severity</span>
        <select value={severity} onChange={(event) => onSeverityChange(event.target.value)}>
          <option value="ALL">All</option>
          <option value="INFO">INFO</option>
          <option value="WARNING">WARNING</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
      </label>
      <label className="field compact-field">
        <span>Phase</span>
        <select value={phase} onChange={(event) => onPhaseChange(event.target.value)}>
          <option value="ALL">All</option>
          <option value="STARTED">STARTED</option>
          <option value="ONGOING">ONGOING</option>
          <option value="RECOVERED">RECOVERED</option>
        </select>
      </label>
      <span className="filter-counter">{visible}/{total} events</span>
    </div>
  );
}

function IncidentsPanel({ events }: { events: DriftEvent[] }) {
  const incidents = buildIncidents(events);
  const active = incidents.filter((incident) => !incident.recoveredAt);
  const recovered = incidents.filter((incident) => incident.recoveredAt);

  return (
    <Panel title="Drift incidents">
      {incidents.length === 0 ? (
        <div className="empty-state">Активных или завершённых drift episodes пока нет.</div>
      ) : (
        <div className="incident-layout">
          <IncidentColumn title="Active incidents" incidents={active} emptyText="Активных incident-ов нет." />
          <IncidentColumn title="Recovered incidents" incidents={recovered} emptyText="Завершённых incident-ов нет." />
        </div>
      )}
    </Panel>
  );
}

function IncidentColumn({ title, incidents, emptyText }: { title: string; incidents: DriftIncident[]; emptyText: string }) {
  return (
    <div className="incident-column">
      <h3>{title}</h3>
      {incidents.length === 0 ? (
        <div className="empty-state compact">{emptyText}</div>
      ) : (
        <div className="incident-list">
          {incidents.map((incident) => (
            <article className={incident.recoveredAt ? "incident-card recovered" : "incident-card active"} key={incident.id}>
              <div className="incident-head">
                <div>
                  <strong>{incident.service}</strong>
                  <span>{incident.metric} · {incident.operation || "-"}</span>
                </div>
                <span className={`severity ${incident.severity.toLowerCase()}`}>{incident.severity}</span>
              </div>
              <dl className="incident-meta">
                <dt>Detector</dt>
                <dd>{incident.detector}</dd>
                <dt>Started</dt>
                <dd>{formatMoscow(incident.startedAt)}</dd>
                <dt>Duration</dt>
                <dd>{formatDuration(incident.startedAt, incident.recoveredAt)}</dd>
              </dl>
              <p>{incident.explanation}</p>
            </article>
          ))}
        </div>
      )}
    </div>
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

function BenchmarkPanel({
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

function StoredEventsTable({ storedEvents }: { storedEvents: DemoStoredDriftEvent[] }) {
  if (storedEvents.length === 0) {
    return <div className="empty-state compact">Сохранённых drift events пока нет.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Received MSK</th>
            <th>Source</th>
            <th>Run</th>
            <th>Severity</th>
            <th>Phase</th>
            <th>Service</th>
            <th>Metric</th>
            <th>Detector</th>
            <th>Explanation</th>
          </tr>
        </thead>
        <tbody>
          {storedEvents.map((stored) => {
            const event = stored.event;
            return (
              <tr key={`${stored.source}-${stored.runId}-${event.id}`}>
                <td>{formatMoscow(stored.receivedAt)}</td>
                <td><span className="badge">{stored.source}</span></td>
                <td>{stored.runId}</td>
                <td><span className={`severity ${event.severity.toLowerCase()}`}>{event.severity}</span></td>
                <td><span className={`phase ${event.phase.toLowerCase()}`}>{event.phase}</span></td>
                <td>{event.key.service}</td>
                <td>{event.key.metric}</td>
                <td>{event.detector}</td>
                <td className="event-explanation">{eventExplanation(event)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
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

function Notice({ tone, text }: { tone: "info" | "error"; text: string }) {
  return (
    <div className={`notice ${tone}`}>
      {tone === "error" ? <AlertCircle size={16} /> : <Activity size={16} />}
      <span>{text}</span>
    </div>
  );
}

function StatusPill({ label, active }: { label: string; active: boolean }) {
  return <span className={active ? "status-pill active" : "status-pill"}>{label}: {active ? "running" : "idle"}</span>;
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

function eventMatchesQuery(event: DriftEvent, query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return true;
  }

  return [
    event.id,
    event.key.service,
    event.key.metric,
    event.key.instance,
    event.key.operation,
    event.detector,
    event.algorithm,
    event.phase,
    event.severity,
    event.reason,
    eventExplanation(event)
  ]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(normalized));
}

function countSeverity(events: DriftEvent[], severity: string) {
  return events.filter((event) => event.severity === severity).length;
}

interface DriftIncident {
  id: string;
  service: string;
  metric: string;
  operation?: string;
  detector: string;
  severity: string;
  startedAt: string;
  recoveredAt?: string;
  explanation: string;
}

function buildIncidents(events: DriftEvent[]) {
  const incidents = new Map<string, DriftIncident>();

  for (const event of events.slice().sort((left, right) => left.detectedAt.localeCompare(right.detectedAt))) {
    const id = incidentId(event);
    const current = incidents.get(id);

    if (event.phase === "RECOVERED") {
      if (current) {
        incidents.set(id, { ...current, recoveredAt: event.detectedAt });
      } else {
        incidents.set(id, eventToIncident(event, event.detectedAt));
      }
      continue;
    }

    if (!current) {
      incidents.set(id, eventToIncident(event));
      continue;
    }

    if (severityRank(event.severity) > severityRank(current.severity)) {
      incidents.set(id, {
        ...current,
        severity: event.severity,
        explanation: eventExplanation(event)
      });
    }
  }

  return [...incidents.values()].sort((left, right) => right.startedAt.localeCompare(left.startedAt));
}

function eventToIncident(event: DriftEvent, recoveredAt?: string): DriftIncident {
  return {
    id: incidentId(event),
    service: event.key.service,
    metric: event.key.metric,
    operation: event.key.operation,
    detector: event.detector,
    severity: event.severity,
    startedAt: event.detectedAt,
    recoveredAt,
    explanation: eventExplanation(event)
  };
}

function incidentId(event: DriftEvent) {
  return `${streamId(event.key)}|${event.detector}`;
}

function severityRank(severity: string) {
  return severity === "CRITICAL" ? 3 : severity === "WARNING" ? 2 : severity === "INFO" ? 1 : 0;
}

function eventExplanation(event: DriftEvent) {
  const current = detailNumber(event, "currentMean") ?? event.currentValue;
  const baseline = detailNumber(event, "baselineMean") ?? event.baselineValue;
  const relative = detailNumber(event, "relativeChangePercent");
  const pValue = detailNumber(event, "pValue");
  const statistic = detailNumber(event, "statistic") ?? detailNumber(event, "chiSquare");
  const threshold = event.severity === "CRITICAL"
    ? detailNumber(event, "criticalThreshold")
    : detailNumber(event, "warningThreshold");

  const parts = [
    `Current ${formatNumber(current)} vs baseline ${formatNumber(baseline)}`,
    relative === undefined ? undefined : `${relative >= 0 ? "+" : ""}${formatNumber(relative)}%`,
    threshold === undefined ? undefined : `threshold ${formatNumber(threshold)}`,
    pValue === undefined ? undefined : `p-value ${formatNumber(pValue)}`,
    statistic === undefined ? undefined : `statistic ${formatNumber(statistic)}`
  ].filter(Boolean);

  return `${parts.join(" · ")}. ${event.reason}`;
}

function detailNumber(event: DriftEvent, key: string) {
  const value = event.details?.[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function formatNumber(value?: number) {
  return value !== undefined && Number.isFinite(value)
    ? new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 3 }).format(value)
    : "—";
}

function formatPercent(value: number) {
  return `${formatNumber(value * 100)}%`;
}

function boolCount(value?: boolean) {
  return value ? 1 : 0;
}

function legacyPrecision(metrics: { falsePositive?: boolean }) {
  return metrics.falsePositive ? 0 : 1;
}

function legacyRecall(metrics: { missed?: boolean }) {
  return metrics.missed ? 0 : 1;
}

function formatMoscow(value: string) {
  return new Intl.DateTimeFormat("ru-RU", {
    timeZone: "Europe/Moscow",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(new Date(value));
}

function formatDuration(start: string, end?: string) {
  const endTime = end ? new Date(end).getTime() : Date.now();
  const seconds = Math.max(0, Math.round((endTime - new Date(start).getTime()) / 1000));
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  if (minutes < 60) {
    return `${minutes}m ${rest}s`;
  }
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

function readableError(error: unknown) {
  return error instanceof Error ? error.message : "Unknown error";
}
