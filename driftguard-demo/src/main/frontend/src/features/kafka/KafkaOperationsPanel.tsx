import { MetricCard, Panel } from "../../components/ui";
import { formatNumber } from "../../lib/format";
import type { KafkaDemoStatus, KafkaOperationsSnapshot } from "../../types";

export function KafkaOperationsPanel({ status, operations }: { status?: KafkaDemoStatus; operations?: KafkaOperationsSnapshot }) {
  const producerCount = status?.producers.length ?? 0;
  const activeProducers = status?.producers.filter((producer) => producer.running).length ?? 0;
  const progressValue = status?.totalPoints ? Math.round(((status.producedPoints ?? 0) / status.totalPoints) * 100) : 0;
  const lastEvent = status?.consumedEvents.at(-1);

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
        <MetricCard title="Events" value={formatNumber(operations?.metrics.emittedEvents ?? status?.consumedEvents.length)} helper="DriftEvent в pipeline" />
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
