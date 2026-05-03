import { Activity, CheckCircle2, RotateCcw, ShieldAlert } from "lucide-react";
import type { ReactNode } from "react";
import { MetricCard, Panel } from "../../components/ui";
import { formatNumber } from "../../lib/format";
import type { DriftEvent, KafkaDemoStatus, KafkaOperationsSnapshot } from "../../types";

export function KafkaOperationsPanel({ status, operations }: { status?: KafkaDemoStatus; operations?: KafkaOperationsSnapshot }) {
  const producerCount = status?.producers.length ?? 0;
  const activeProducers = status?.producers.filter((producer) => producer.running).length ?? 0;
  const progressValue = status?.totalPoints ? Math.round(((status.producedPoints ?? 0) / status.totalPoints) * 100) : 0;
  const events = status?.consumedEvents ?? [];
  const lastEvent = events.at(-1);
  const lifecycle = eventLifecycle(events);
  const hasRecovered = lifecycle.recovered > 0;
  const hasOngoing = lifecycle.ongoing > 0;

  return (
    <Panel title="Kafka operations" className="ops-panel">
      <div className={status?.running ? "ops-hero active" : "ops-hero"}>
        <div>
          <p className="eyebrow">Stateful Kafka pipeline</p>
          <h2>{status?.running ? "Pipeline is running" : "Pipeline is idle"}</h2>
          <p>
            {status?.scenario ?? "Scenario не выбран"} · {status?.replay ? "Replay" : "Normal"} · {status?.speed ?? 1}x
          </p>
        </div>
        <div className="ops-progress-ring" aria-label={`Replay progress ${progressValue}%`}>
          <strong>{progressValue}%</strong>
          <span>{status?.producedPoints ?? 0}/{status?.totalPoints ?? 0}</span>
        </div>
      </div>

      <div className="ops-kpis">
        <MetricCard title="Processed" value={formatNumber(operations?.metrics.processedPoints)} helper="MetricPoint обработано" />
        <MetricCard title="Events" value={formatNumber(operations?.metrics.emittedEvents ?? events.length)} helper="DriftEvent в pipeline" />
        <MetricCard
          title="Errors / DLQ"
          value={`${formatNumber(operations?.metrics.failedPoints)} / ${formatNumber(operations?.metrics.routedErrors)}`}
          helper="Ошибки detector-а"
          tone={(operations?.metrics.failedPoints ?? 0) > 0 ? "danger" : undefined}
        />
        <MetricCard
          title="Latency"
          value={`${formatNumber(operations?.metrics.meanDurationMillis)} ms`}
          helper={operations?.telemetryEnabled ? "Micrometer timer" : "Telemetry недоступна"}
        />
      </div>

      <div className="ops-lifecycle-grid">
        <LifecycleTile icon={<ShieldAlert size={18} />} label="Started" value={lifecycle.started} helper="Новые drift episodes" />
        <LifecycleTile icon={<Activity size={18} />} label="Ongoing" value={lifecycle.ongoing} helper={hasOngoing ? "Обновления активных episodes" : "Появятся после cooldown"} />
        <LifecycleTile icon={<CheckCircle2 size={18} />} label="Recovered" value={lifecycle.recovered} helper={hasRecovered ? "Закрытые episodes" : "Ждём нормализацию потока"} />
        <LifecycleTile icon={<RotateCcw size={18} />} label="Active" value={lifecycle.active} helper="Сейчас открыто в UI" />
      </div>

      <div className="ops-meta-row">
        <span><strong>Producers</strong>{activeProducers}/{producerCount}</span>
        <span><strong>Input</strong>{operations?.streamsInputTopics?.join(", ") || status?.inputTopic || "—"}</span>
        <span><strong>Output</strong>{operations?.outputTopic || status?.outputTopic || "—"}</span>
        <span><strong>State store</strong>{operations?.runtimeStateStoreName ?? "—"}</span>
        <span><strong>Error mode</strong>{operations?.detectionErrorMode ?? "—"}</span>
        <span><strong>Last event</strong>{lastEvent ? `${lastEvent.phase} · ${lastEvent.key.metric}` : "—"}</span>
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

function LifecycleTile({ icon, label, value, helper }: { icon: ReactNode; label: string; value: number; helper: string }) {
  return (
    <div className="lifecycle-tile">
      <span className="lifecycle-icon">{icon}</span>
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
        <small>{helper}</small>
      </div>
    </div>
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

function eventLifecycle(events: DriftEvent[]) {
  const started = events.filter((event) => event.phase === "STARTED").length;
  const ongoing = events.filter((event) => event.phase === "ONGOING").length;
  const recovered = events.filter((event) => event.phase === "RECOVERED").length;
  const active = new Set<string>();

  for (const event of events) {
    const key = `${event.key.service}|${event.key.metric}|${event.key.operation ?? ""}|${event.detector}`;
    if (event.phase === "RECOVERED") {
      active.delete(key);
    } else {
      active.add(key);
    }
  }

  return { started, ongoing, recovered, active: active.size };
}
